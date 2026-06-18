package wpn.hdri.ss.http;

import org.junit.Test;
import wpn.hdri.ss.data.Method;
import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.data2.Interpolation;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.engine2.RangeAnalyzer;
import wpn.hdri.ss.writer.InMemoryWriter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Reproduces the real demo-stand config shape (a single bound configured alone, not both)
 * end-to-end through the live HTTP dashboard, to lock in that either bound independently
 * triggers the {@code range-warn} row highlight.
 */
public class MetricsServerRangeHighlightTest {

    private static Attribute<Double> attr(int id, String name) {
        return new Attribute<>(id, null, 0L, Method.EventType.NONE, Double.class,
                name, "tango://localhost:10000/sr/demo/controller/" + name, name, Interpolation.LAST);
    }

    private static String fetchStatusPage(MetricsServer server) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + server.getPort() + "/")).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    /** Table body only — the {@code <style>} block always contains the literal "range-warn" CSS rule. */
    private static String tableBody(String html) {
        int i = html.indexOf("<tbody>");
        return i < 0 ? "" : html.substring(i);
    }

    @Test
    public void highlightsRowWhenOnlyMinConfiguredAndBreached() throws Exception {
        Attribute<Double> beamCurrent = attr(0, "BeamCurrent");
        InMemoryWriter inMemory = new InMemoryWriter(1);
        inMemory.onEvent(new SingleRecord<>(beamCurrent, 1000L, 1000L, 150.0)); // below min=200, no max configured

        Map<String, RangeAnalyzer.Bounds> bounds = Map.of("BeamCurrent", new RangeAnalyzer.Bounds(200.0, null));
        MetricsServer server = new MetricsServer(0, inMemory, bounds);
        server.start();
        try {
            String html = fetchStatusPage(server);
            assertTrue("expected range-warn row for min-only breach", tableBody(html).contains("range-warn"));
        } finally {
            server.stop();
        }
    }

    @Test
    public void highlightsRowWhenOnlyMaxConfiguredAndBreached() throws Exception {
        Attribute<Double> temperature = attr(0, "Temperature");
        InMemoryWriter inMemory = new InMemoryWriter(1);
        inMemory.onEvent(new SingleRecord<>(temperature, 1000L, 1000L, 999.0)); // above max=100, no min configured

        Map<String, RangeAnalyzer.Bounds> bounds = Map.of("Temperature", new RangeAnalyzer.Bounds(null, 100.0));
        MetricsServer server = new MetricsServer(0, inMemory, bounds);
        server.start();
        try {
            String html = fetchStatusPage(server);
            assertTrue("expected range-warn row for max-only breach", tableBody(html).contains("range-warn"));
        } finally {
            server.stop();
        }
    }

    @Test
    public void noHighlightWhenOnlyMinConfiguredAndWithinRange() throws Exception {
        Attribute<Double> beamCurrent = attr(0, "BeamCurrent");
        InMemoryWriter inMemory = new InMemoryWriter(1);
        inMemory.onEvent(new SingleRecord<>(beamCurrent, 1000L, 1000L, 250.0)); // above min=200, no max configured

        Map<String, RangeAnalyzer.Bounds> bounds = Map.of("BeamCurrent", new RangeAnalyzer.Bounds(200.0, null));
        MetricsServer server = new MetricsServer(0, inMemory, bounds);
        server.start();
        try {
            String html = fetchStatusPage(server);
            assertFalse("did not expect range-warn row when within range", tableBody(html).contains("range-warn"));
        } finally {
            server.stop();
        }
    }

    @Test
    public void highlightsRowWhenBothConfiguredAndEitherBreached() throws Exception {
        Attribute<Double> pressure = attr(0, "Pressure");
        InMemoryWriter inMemory = new InMemoryWriter(1);
        inMemory.onEvent(new SingleRecord<>(pressure, 1000L, 1000L, 5.0)); // below min=10 of [10,50]

        Map<String, RangeAnalyzer.Bounds> bounds = Map.of("Pressure", new RangeAnalyzer.Bounds(10.0, 50.0));
        MetricsServer server = new MetricsServer(0, inMemory, bounds);
        server.start();
        try {
            String html = fetchStatusPage(server);
            assertTrue("expected range-warn row for both-bounds, min-breach", tableBody(html).contains("range-warn"));
        } finally {
            server.stop();
        }
    }
}
