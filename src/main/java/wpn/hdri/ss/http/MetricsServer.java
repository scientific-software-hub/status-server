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

    private static final String METRIC_NAME = "control_system_attribute";
    private static final String CONTENT_TYPE_PROMETHEUS = "text/plain; version=0.0.4; charset=utf-8";
    private static final String CONTENT_TYPE_TEXT = "text/plain; charset=utf-8";

    private final HttpServer server;
    private volatile boolean ready = false;

    public MetricsServer(int port, InMemoryWriter inMemory) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/metrics", exchange -> handle(exchange, () -> buildMetrics(inMemory.getStorage())));
        server.createContext("/health",  exchange -> handle(exchange, () -> "OK"));
        server.createContext("/ready",   exchange -> handle(exchange, () -> ready ? "READY" : null));

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
                    ? CONTENT_TYPE_PROMETHEUS : CONTENT_TYPE_TEXT;
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
        StringBuilder sb = new StringBuilder(4096);

        sb.append("# HELP ").append(METRIC_NAME).append("_value Latest numeric value of a monitored control system attribute\n");
        sb.append("# TYPE ").append(METRIC_NAME).append("_value gauge\n");

        sb.append("# HELP ").append(METRIC_NAME).append("_state Latest string/enum state of a monitored control system attribute\n");
        sb.append("# TYPE ").append(METRIC_NAME).append("_state gauge\n");

        for (SingleRecord<?> record : snapshot) {
            if (record == null || record.value == null) continue;

            String attrLabel = sanitize(record.attribute.fullName);
            String aliasLabel = sanitize(record.attribute.alias);
            long timestamp = record.r_t;

            if (record.value instanceof Number num) {
                sb.append(METRIC_NAME).append("_value{")
                        .append("attribute=\"").append(attrLabel).append('"')
                        .append(",alias=\"").append(aliasLabel).append('"')
                        .append("} ").append(num.doubleValue())
                        .append(' ').append(timestamp).append('\n');
            } else {
                // String / enum — encode as labelled gauge with value=1
                sb.append(METRIC_NAME).append("_state{")
                        .append("attribute=\"").append(attrLabel).append('"')
                        .append(",alias=\"").append(aliasLabel).append('"')
                        .append(",state=\"").append(escape(record.value.toString())).append('"')
                        .append("} 1")
                        .append(' ').append(timestamp).append('\n');
            }
        }

        return sb.toString();
    }

    /** Replace characters not allowed in Prometheus label values with '_'. */
    private static String sanitize(String s) {
        if (s == null) return "unknown";
        return s.replaceAll("[^a-zA-Z0-9_:/.]", "_");
    }

    /** Escape backslashes, double-quotes, and newlines in label values. */
    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
