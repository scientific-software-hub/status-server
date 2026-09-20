package wpn.hdri.ss.http;

import org.junit.Test;
import wpn.hdri.ss.data.Method;
import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.data2.Interpolation;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.writer.InMemoryWriter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * A "Stalled" record (non-null last value, {@code failureType == "Stalled"}, written by the
 * engine's stall watchdog) must render amber with a STALE badge on the status page and report
 * {@code _up=0} / {@code _stale=1} in Prometheus output, while still exporting the last known
 * value — this is the fix for attributes reporting UP forever after their poller died.
 */
public class MetricsServerStaleTest {

    private static Attribute<Double> attr(int id, String name) {
        return new Attribute<>(id, null, 1000L, Method.EventType.NONE, Double.class,
                name, "tango://localhost:10000/sr/demo/controller/" + name, name, Interpolation.LAST);
    }

    private static String fetch(MetricsServer server, String path) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + server.getPort() + path)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private static String tableBody(String html) {
        int i = html.indexOf("<tbody>");
        return i < 0 ? "" : html.substring(i);
    }

    /** First Prometheus line for {@code metricPrefix{...}} whose label set contains {@code labelSubstring}. */
    private static String findLine(String metrics, String metricPrefix, String labelSubstring) {
        for (String line : metrics.split("\n")) {
            if (line.startsWith(metricPrefix + "{") && line.contains(labelSubstring)) {
                return line;
            }
        }
        return null;
    }

    @Test
    public void stalledRecordRendersAmberWithStaleBadge() throws Exception {
        Attribute<Double> attribute = attr(0, "BeamCurrent");
        InMemoryWriter inMemory = new InMemoryWriter(1);
        long oldTimestamp = System.currentTimeMillis() - 600_000;
        inMemory.onEvent(new SingleRecord<>(attribute, oldTimestamp, oldTimestamp, 42.0, "Stalled", "no update for 600000 ms"));

        MetricsServer server = new MetricsServer(0, inMemory, Map.of());
        server.start();
        try {
            String body = tableBody(fetch(server, "/"));
            assertTrue("expected a stale-classed row", body.contains("class=\"stale"));
            assertTrue("expected a STALE badge", body.contains(">STALE<"));
            assertTrue("last known value must still be shown", body.contains("42.0"));
        } finally {
            server.stop();
        }
    }

    @Test
    public void stalledRecordReportsUpZeroAndStaleOneInMetrics() throws Exception {
        Attribute<Double> attribute = attr(0, "Temperature");
        InMemoryWriter inMemory = new InMemoryWriter(1);
        long oldTimestamp = System.currentTimeMillis() - 600_000;
        inMemory.onEvent(new SingleRecord<>(attribute, oldTimestamp, oldTimestamp, 99.0, "Stalled", "no update for 600000 ms"));

        MetricsServer server = new MetricsServer(0, inMemory, Map.of());
        server.start();
        try {
            String metrics = fetch(server, "/metrics");

            String upLine = findLine(metrics, "control_system_attribute_up", "name=\"Temperature\"");
            assertNotNull("expected an _up line for Temperature", upLine);
            assertTrue("stalled attribute must report _up=0: " + upLine, upLine.endsWith("} 0"));
            assertTrue("stalled _up line must carry the failure classification", upLine.contains("failure_type=\"Stalled\""));

            String staleLine = findLine(metrics, "control_system_attribute_stale", "name=\"Temperature\"");
            assertNotNull("expected a _stale line for Temperature", staleLine);
            assertTrue("stalled attribute must report _stale=1: " + staleLine, staleLine.endsWith("} 1"));

            assertNotNull("last known value must still be exported",
                    findLine(metrics, "control_system_attribute_value", "name=\"Temperature\""));
        } finally {
            server.stop();
        }
    }

    @Test
    public void freshRecordReportsUpOneAndStaleZero() throws Exception {
        Attribute<Double> attribute = attr(0, "Pressure");
        InMemoryWriter inMemory = new InMemoryWriter(1);
        long now = System.currentTimeMillis();
        inMemory.onEvent(new SingleRecord<>(attribute, now, now, 5.0));

        MetricsServer server = new MetricsServer(0, inMemory, Map.of());
        server.start();
        try {
            String metrics = fetch(server, "/metrics");

            String upLine = findLine(metrics, "control_system_attribute_up", "name=\"Pressure\"");
            assertNotNull(upLine);
            assertTrue(upLine.endsWith("} 1"));
            assertFalse(upLine.contains("failure_type"));

            String staleLine = findLine(metrics, "control_system_attribute_stale", "name=\"Pressure\"");
            assertNotNull(staleLine);
            assertTrue(staleLine.endsWith("} 0"));

            String body = tableBody(fetch(server, "/"));
            assertFalse("fresh row must not be stale-classed", body.contains("class=\"stale"));
        } finally {
            server.stop();
        }
    }

    @Test
    public void failedReadStillReportsDownNotStale() throws Exception {
        Attribute<Double> attribute = attr(0, "Vacuum");
        InMemoryWriter inMemory = new InMemoryWriter(1);
        inMemory.onEvent(new SingleRecord<>(attribute, System.currentTimeMillis(), 0L, null, "ConnectionRefused", null));

        MetricsServer server = new MetricsServer(0, inMemory, Map.of());
        server.start();
        try {
            String metrics = fetch(server, "/metrics");
            assertNull("a null-value record has no _stale line at all",
                    findLine(metrics, "control_system_attribute_stale", "name=\"Vacuum\""));

            String body = tableBody(fetch(server, "/"));
            assertTrue("connection-refused row must render as down, not stale", body.contains("class=\"down"));
        } finally {
            server.stop();
        }
    }
}
