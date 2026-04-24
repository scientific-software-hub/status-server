package wpn.hdri.ss.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.data2.Snapshot;
import wpn.hdri.ss.engine2.DataStorage;
import wpn.hdri.ss.event.AvailabilityState;
import wpn.hdri.ss.writer.InMemoryWriter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Lightweight HTTP server exposing operational endpoints.
 *
 * <ul>
 *   <li>{@code GET /metrics} – Prometheus text format snapshot (for ops dashboards / alerting)</li>
 *   <li>{@code GET /health}  – liveness probe, always 200 while running</li>
 *   <li>{@code GET /ready}   – readiness probe, 200 once the engine has started</li>
 * </ul>
 */
public class MetricsServer {

    private static final Logger logger = LoggerFactory.getLogger(MetricsServer.class);

    private static final String METRIC_PREFIX = "control_system_attribute";
    private static final String CONTENT_TYPE_PROMETHEUS = "text/plain; version=0.0.4; charset=utf-8";
    private static final String CONTENT_TYPE_TEXT = "text/plain; charset=utf-8";
    private static final String CONTENT_TYPE_HTML = "text/html; charset=utf-8";
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final HttpServer server;
    private volatile boolean ready = false;

    public MetricsServer(int port, InMemoryWriter inMemory) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/metrics", exchange -> handle(exchange, CONTENT_TYPE_PROMETHEUS,
                () -> buildMetrics(inMemory.getStorage())));
        server.createContext("/", exchange -> handle(exchange, CONTENT_TYPE_HTML,
                () -> buildStatusPage(inMemory.getStorage())));
        server.createContext("/health", exchange -> handle(exchange, CONTENT_TYPE_TEXT, () -> "OK"));
        server.createContext("/ready", exchange -> handle(exchange, CONTENT_TYPE_TEXT, () -> ready ? "READY" : null));

        // Each request handled by a virtual thread — no blocking the selector
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }

    public void start() {
        server.start();
        logger.info("HTTP server listening on port {}", server.getAddress().getPort());
    }

    public void markReady() {
        ready = true;
    }

    public void stop() {
        server.stop(1);
    }

    // --- helpers ---

    private interface BodySupplier {
        /** Returns the body string, or null to respond with 503. */
        String get() throws Exception;
    }

    private void handle(HttpExchange exchange, String contentType, BodySupplier supplier) throws IOException {
        try (exchange) {
            try {
                String body = supplier.get();
                if (body == null) {
                    exchange.sendResponseHeaders(503, -1);
                    return;
                }
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (Exception e) {
                logger.error("Error handling {}: {}", exchange.getRequestURI(), e.getMessage(), e);
                exchange.sendResponseHeaders(500, -1);
            }
        }
    }

    private String buildStatusPage(DataStorage storage) {
        Snapshot snapshot = storage.getSnapshot();
        long nowMillis = System.currentTimeMillis();
        String timestamp = TS_FMT.format(Instant.now());

        record Row(String device, String attrName, String alias, String value,
                   String age, AvailabilityState state, String failureType, String failureDetail) {}

        List<Row> rows = new ArrayList<>();
        int countUp = 0, countDown = 0;

        for (SingleRecord<?> record : snapshot) {
            if (record == null || record.attribute == null) continue;

            AttributeParts parts = splitAttribute(record.attribute.fullName);
            String fullDeviceName = parts.source + "://" + parts.device;
            String alias = record.attribute.alias != null ? record.attribute.alias : "";
            AvailabilityState state = record.value != null ? AvailabilityState.UP : AvailabilityState.DOWN;

            if (state == AvailabilityState.UP) countUp++; else countDown++;

            String value, age;
            if (record.value == null) {
                value = null;
                age = null;
            } else {
                value = htmlEscape(record.value.toString());
                double ageSeconds = Math.max(0.0, (nowMillis - record.r_t) / 1000.0);
                age = ageSeconds < 60
                        ? String.format("%.1fs", ageSeconds)
                        : String.format("%.0fm %.0fs", Math.floor(ageSeconds / 60), ageSeconds % 60);
            }

            rows.add(new Row(fullDeviceName, parts.name, alias, value, age, state,
                    record.failureType, record.failureDetail));
        }

        rows.sort(Comparator.comparing(Row::device).thenComparing(Row::attrName));

        int total = countUp + countDown;

        StringBuilder sb = new StringBuilder(16384);
        sb.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">")
          .append("<title>StatusServer</title>")
          .append("<style>")
          .append("body{font-family:monospace;font-size:13px;margin:1em 2em;background:#1a1a1a;color:#e0e0e0}")
          .append("h1{font-size:1.1em;margin-bottom:0.3em}")
          .append(".meta{color:#888;font-size:0.9em;margin-bottom:1em}")
          .append(".summary{margin-bottom:1em;display:flex;gap:1.5em}")
          .append(".badge{padding:2px 8px;border-radius:3px;font-weight:bold}")
          .append(".up{background:#1a4a1a;color:#4caf50}")
          .append(".stale{background:#4a3a00;color:#ffc107}")
          .append(".down{background:#4a1a1a;color:#f44336}")
          .append("table{border-collapse:collapse;width:100%}")
          .append("th{text-align:left;padding:4px 8px;border-bottom:1px solid #444;color:#aaa}")
          .append("td{padding:4px 8px;border-bottom:1px solid #2a2a2a;white-space:nowrap}")
          .append("tr.up td{background:#1a2a1a}")
          .append("tr.stale td{background:#2a2000}")
          .append("tr.down td{background:#2a1010}")
          .append(".failure{color:#f44336;font-size:0.85em}")
          .append(".detail{color:#888;font-size:0.8em}")
          .append(".sep td{border-top:1px solid #333;padding-top:6px}")
          .append("</style></head><body>")
          .append("<h1>StatusServer &mdash; Live Attribute Status</h1>")
          .append("<div id=\"content\">")
          .append("<p class=\"meta\">Last updated: ").append(timestamp)
          .append(" &nbsp;|&nbsp; auto-refresh every 5 s</p>")
          .append("<div class=\"summary\">")
          .append("<span>Monitored: <strong>").append(total).append("</strong></span>")
          .append("<span class=\"badge up\">UP ").append(countUp).append("</span>")
          .append("<span class=\"badge down\">DOWN ").append(countDown).append("</span>")
          .append("</div>")
          .append("<table><thead><tr>")
          .append("<th>Device</th><th>Attribute</th><th>Alias</th><th>Value</th><th>Age</th><th>State</th>")
          .append("</tr></thead><tbody>");

        String prevDevice = null;
        for (Row row : rows) {
            String stateClass = row.state().name().toLowerCase();
            String sep = !row.device().equals(prevDevice) && prevDevice != null ? " sep" : "";
            prevDevice = row.device();

            sb.append("<tr class=\"").append(stateClass).append(sep).append("\">")
              .append("<td>").append(htmlEscape(row.device())).append("</td>")
              .append("<td>").append(htmlEscape(row.attrName())).append("</td>")
              .append("<td>").append(htmlEscape(row.alias())).append("</td>");

            if (row.value() != null) {
                sb.append("<td>").append(row.value()).append("</td>")
                  .append("<td>").append(row.age()).append("</td>");
            } else {
                sb.append("<td>");
                if (row.failureType() != null) {
                    sb.append("<span class=\"failure\">").append(htmlEscape(row.failureType())).append("</span>");
                    if (row.failureDetail() != null && !row.failureDetail().isBlank()) {
                        sb.append("<br><span class=\"detail\">").append(htmlEscape(row.failureDetail())).append("</span>");
                    }
                } else {
                    sb.append("&mdash;");
                }
                sb.append("</td><td>&mdash;</td>");
            }

            sb.append("<td><span class=\"badge ").append(stateClass).append("\">")
              .append(row.state().name()).append("</span></td>")
              .append("</tr>");
        }

        sb.append("</tbody></table>")
          .append("</div>") // #content
          .append("<script>")
          .append("setInterval(async()=>{")
          .append("try{")
          .append("const r=await fetch('/status');")
          .append("const t=await r.text();")
          .append("const d=new DOMParser().parseFromString(t,'text/html');")
          .append("const nc=d.getElementById('content');")
          .append("if(nc)document.getElementById('content').replaceWith(nc);")
          .append("}catch(e){console.warn('status refresh failed',e);}")
          .append("},5000);")
          .append("</script>")
          .append("</body></html>");

        return sb.toString();
    }

    private String buildMetrics(DataStorage storage) {
        Snapshot snapshot = storage.getSnapshot();
        StringBuilder sb = new StringBuilder(8192);
        long nowMillis = System.currentTimeMillis();

        // --- metric headers ---
        sb.append("# HELP ").append(METRIC_PREFIX).append("_value Latest numeric value of a monitored control system attribute\n");
        sb.append("# TYPE ").append(METRIC_PREFIX).append("_value gauge\n");

        sb.append("# HELP ").append(METRIC_PREFIX).append("_state Latest string/enum state of a monitored control system attribute\n");
        sb.append("# TYPE ").append(METRIC_PREFIX).append("_state gauge\n");

        sb.append("# HELP ").append(METRIC_PREFIX).append("_status Human-readable status text of a monitored control system attribute\n");
        sb.append("# TYPE ").append(METRIC_PREFIX).append("_status gauge\n");

        sb.append("# HELP ").append(METRIC_PREFIX).append("_source_timestamp_seconds Source/read timestamp reported by Status Server for the latest sample\n");
        sb.append("# TYPE ").append(METRIC_PREFIX).append("_source_timestamp_seconds gauge\n");

        sb.append("# HELP ").append(METRIC_PREFIX).append("_age_seconds Age of the latest sample in seconds\n");
        sb.append("# TYPE ").append(METRIC_PREFIX).append("_age_seconds gauge\n");

        sb.append("# HELP ").append(METRIC_PREFIX).append("_up 1 if the last read succeeded, 0 if it failed\n");
        sb.append("# TYPE ").append(METRIC_PREFIX).append("_up gauge\n");

        int monitored = 0;
        int up = 0;

        for (SingleRecord<?> record : snapshot) {
            if (record == null || record.attribute == null) {
                continue;
            }

            monitored++;
            AttributeParts parts = splitAttribute(record.attribute.fullName);
            String alias = sanitize(record.attribute.alias);

            String commonLabels = new StringBuilder(256)
                    .append("source=\"").append(parts.source).append('"')
                    .append(",device=\"").append(parts.device).append('"')
                    .append(",name=\"").append(parts.name).append('"')
                    .append(",attribute=\"").append(parts.full).append('"')
                    .append(",alias=\"").append(alias).append('"')
                    .toString();

            if (record.value == null) {
                // last read failed — emit _up=0 with failure classification labels
                sb.append(METRIC_PREFIX).append("_up{")
                        .append(commonLabels)
                        .append(buildFailureLabels(record.failureType, record.failureDetail))
                        .append("} 0\n");
                continue;
            }

            up++;

            // _up=1 for healthy attributes
            sb.append(METRIC_PREFIX).append("_up{")
                    .append(commonLabels)
                    .append("} 1\n");

            double sourceTimestampSeconds = record.r_t / 1000.0d;
            double ageSeconds = Math.max(0.0d, (nowMillis - record.r_t) / 1000.0d);

            // numeric value
            if (record.value instanceof Number num) {
                sb.append(METRIC_PREFIX).append("_value{")
                        .append(commonLabels)
                        .append("} ")
                        .append(num.doubleValue())
                        .append('\n');
            } else {
                String valueStr = record.value.toString();

                boolean isShortState =
                        valueStr.length() < 32 &&
                                valueStr.equals(valueStr.toUpperCase()) &&
                                !valueStr.contains(" ");

                if (isShortState) {
                    // SHORT ENUM STATE (RUNNING, FAULT, etc.)
                    sb.append(METRIC_PREFIX).append("_state{")
                            .append(commonLabels)
                            .append(",state=\"").append(escape(valueStr)).append('"')
                            .append("} 1\n");
                } else {
                    // LONG STATUS TEXT
                    sb.append(METRIC_PREFIX).append("_status{")
                            .append(commonLabels)
                            .append(",status=\"").append(escape(valueStr)).append('"')
                            .append("} 1\n");
                }
            }

            // source/read timestamp as its own metric
            sb.append(METRIC_PREFIX).append("_source_timestamp_seconds{")
                    .append(commonLabels)
                    .append("} ")
                    .append(sourceTimestampSeconds)
                    .append('\n');

            // freshness metric
            sb.append(METRIC_PREFIX).append("_age_seconds{")
                    .append(commonLabels)
                    .append("} ")
                    .append(ageSeconds)
                    .append('\n');
        }

        // --- service-level summary metrics ---
        sb.append("# HELP status_server_monitored_attributes Total number of attributes currently monitored\n");
        sb.append("# TYPE status_server_monitored_attributes gauge\n");
        sb.append("status_server_monitored_attributes ").append(monitored).append('\n');

        sb.append("# HELP status_server_up_attributes Number of attributes whose last read succeeded\n");
        sb.append("# TYPE status_server_up_attributes gauge\n");
        sb.append("status_server_up_attributes ").append(up).append('\n');

        sb.append("# HELP status_server_failed_attributes Number of attributes whose last read failed\n");
        sb.append("# TYPE status_server_failed_attributes gauge\n");
        sb.append("status_server_failed_attributes ").append(monitored - up).append('\n');

        return sb.toString();
    }

    private static String buildFailureLabels(String failureType, String failureDetail) {
        if (failureType == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(",failure_type=\"").append(failureType).append('"');
        if (failureDetail != null && !failureDetail.isBlank()) {
            sb.append(",failure_detail=\"").append(escape(failureDetail)).append('"');
        }
        return sb.toString();
    }

    /**
     * Splits a full attribute path into Prometheus-friendly labels.
     *
     * Examples:
     * tango://localhost:10000/sys/tg_test/1/float_scalar
     *   -> source=tango, device=sys/tg_test/1, name=float_scalar
     *
     * tine:/TEST/JSINESRV/SINEDEV_0/Sine
     *   -> source=tine, device=TEST/JSINESRV/SINEDEV_0, name=Sine
     */
    private static AttributeParts splitAttribute(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new AttributeParts("unknown", "unknown", "unknown", "unknown");
        }

        String normalized = fullName.trim();
        String source = "unknown";
        String remainder = normalized;

        int schemeIdx = normalized.indexOf(':');
        if (schemeIdx > 0) {
            source = sanitize(normalized.substring(0, schemeIdx).toLowerCase());
            remainder = normalized.substring(schemeIdx + 1);
        }

        // strip leading //host:port/ or / when present
        if (remainder.startsWith("//")) {
            int firstSlashAfterHost = remainder.indexOf('/', 2);
            if (firstSlashAfterHost >= 0) {
                remainder = remainder.substring(firstSlashAfterHost + 1);
            } else {
                remainder = "";
            }
        } else if (remainder.startsWith("/")) {
            remainder = remainder.substring(1);
        }

        String device = "unknown";
        String name = "unknown";

        int lastSlash = remainder.lastIndexOf('/');
        if (lastSlash >= 0) {
            device = remainder.substring(0, lastSlash);
            name = remainder.substring(lastSlash + 1);
        } else if (!remainder.isBlank()) {
            name = remainder;
        }

        return new AttributeParts(
                source,
                sanitize(device),
                sanitize(name),
                sanitize(normalized)
        );
    }

    /** Replace problematic characters in label values with '_'. */
    private static String sanitize(String s) {
        if (s == null || s.isBlank()) {
            return "unknown";
        }
        return s.replaceAll("[^a-zA-Z0-9_:/.\\-]", "_");
    }

    /** Escape HTML special characters for safe embedding in HTML content. */
    private static String htmlEscape(String s) {
        if (s == null || s.isBlank()) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /** Escape backslashes, double-quotes, and newlines in label values. */
    private static String escape(String s) {
        if (s == null) {
            return "unknown";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    private record AttributeParts(String source, String device, String name, String full) {
    }
}