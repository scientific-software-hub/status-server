package wpn.hdri.ss.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.data2.Snapshot;
import wpn.hdri.ss.engine2.DataStorage;
import wpn.hdri.ss.writer.InMemoryWriter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
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

    private final HttpServer server;
    private volatile boolean ready = false;

    public MetricsServer(int port, InMemoryWriter inMemory) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/metrics", exchange -> handle(exchange, () -> buildMetrics(inMemory.getStorage())));
        server.createContext("/health", exchange -> handle(exchange, () -> "OK"));
        server.createContext("/ready", exchange -> handle(exchange, () -> ready ? "READY" : null));

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

    private void handle(HttpExchange exchange, BodySupplier supplier) throws IOException {
        try (exchange) {
            String contentType = exchange.getRequestURI().getPath().equals("/metrics")
                    ? CONTENT_TYPE_PROMETHEUS
                    : CONTENT_TYPE_TEXT;
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
                // last read failed — emit _up=0, skip value/timestamp metrics
                sb.append(METRIC_PREFIX).append("_up{")
                        .append(commonLabels)
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