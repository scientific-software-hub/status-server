package wpn.hdri.ss;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.configuration.Device;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.engine2.Engine;
import wpn.hdri.ss.engine2.EngineFactory;
import wpn.hdri.ss.event.EventSink;
import wpn.hdri.ss.event.TechnicalEvent;
import wpn.hdri.ss.http.MetricsServer;
import wpn.hdri.ss.source.DeviceSource;
import wpn.hdri.ss.source.XmlDeviceSource;
import wpn.hdri.ss.writer.InMemoryWriter;
import wpn.hdri.ss.writer.WriterDispatcher;

import java.util.List;

/**
 * Entry point for the status collection server.
 *
 * Usage: StatusServer &lt;path-to-config.xml&gt; [http-port]
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: StatusServer <path-to-config.xml> [http-port]");
            System.exit(1);
        }

        int httpPort = args.length > 1 ? Integer.parseInt(args[1]) : 9190;

        // --- load configuration ---
        StatusServerConfiguration config = StatusServerConfiguration.fromXml(args[0]);
        logger.info("Configuration loaded from {}", args[0]);

        // --- choose device source ---
        if (config.getDevices().isEmpty()) {
            System.err.println("Configuration must contain a non-empty <devices> list.");
            System.exit(1);
            return;
        }
        logger.info("Loading {} device(s) from XML config", config.getDevices().size());
        DeviceSource deviceSource = new XmlDeviceSource(config);

        List<Device> devices = deviceSource.load();
        logger.info("Loaded {} device(s)", devices.size());

        int totalAttributes = devices.stream().mapToInt(d -> d.getAttributes().size()).sum();

        // --- build sink chain ---
        InMemoryWriter inMemory = new InMemoryWriter(totalAttributes, true);
        WriterDispatcher dispatcher = new WriterDispatcher(List.of(inMemory));

        // --- build and start engine ---
        // TODO Step 3: replace no-op with AvailabilityAnalyzer
        EventSink<TechnicalEvent> technicalSink = event -> {};
        EngineFactory factory = new EngineFactory(devices, dispatcher, technicalSink);
        Engine engine = factory.newEngine();

        if (!factory.getFailedAttributes().isEmpty()) {
            logger.warn("Failed attributes will not be monitored: {}", factory.getFailedAttributes());
        }

        // --- start HTTP server ---
        MetricsServer httpServer = new MetricsServer(httpPort, inMemory);
        httpServer.start();

        engine.start();
        httpServer.markReady();
        logger.info("Engine started, ready to collect");

        // --- graceful shutdown ---
        Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(() -> {
            logger.info("Shutting down...");
            engine.stop();
            httpServer.stop();
            try {
                dispatcher.close();
            } catch (Exception e) {
                logger.error("Error closing sinks: {}", e.getMessage(), e);
            }
            logger.info("Shutdown complete");
        }));
    }
}
