package wpn.hdri.ss;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.engine2.Engine;
import wpn.hdri.ss.engine2.EngineFactory;
import wpn.hdri.ss.http.MetricsServer;
import wpn.hdri.ss.source.DeviceSource;
import wpn.hdri.ss.source.FrappeDeviceSource;
import wpn.hdri.ss.source.XmlDeviceSource;
import wpn.hdri.ss.writer.InMemoryWriter;
import wpn.hdri.ss.writer.RecordWriter;
import wpn.hdri.ss.writer.WriterDispatcher;
import wpn.hdri.ss.configuration.Device;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for the status collection server.
 *
 * Usage: StatusServer &lt;path-to-config.xml&gt; [http-port]
 *
 * Configuration routing:
 *   - {@code <frappe>} present  → devices loaded from ERPNext Assets
 *   - {@code <devices>} present → devices loaded from static XML (testing / standalone)
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: StatusServer <path-to-config.xml> [http-port]");
            System.exit(1);
        }

        int httpPort = args.length > 1 ? Integer.parseInt(args[1]) : 9090;

        // --- load configuration ---
        StatusServerConfiguration config = StatusServerConfiguration.fromXml(args[0]);
        logger.info("Configuration loaded from {}", args[0]);

        // --- choose device source ---
        DeviceSource deviceSource;
        if (config.getFrappe() != null) {
            logger.info("Frappe configured — loading devices from ERPNext Assets");
            deviceSource = new FrappeDeviceSource(config.getFrappe());
        } else if (!config.getDevices().isEmpty()) {
            logger.info("Static XML devices configured — loading {} device(s)", config.getDevices().size());
            deviceSource = new XmlDeviceSource(config);
        } else {
            System.err.println("Configuration must contain either <frappe> or a non-empty <devices> list.");
            System.exit(1);
            return;
        }

        List<Device> devices = deviceSource.load();
        logger.info("Loaded {} device(s)", devices.size());

        int totalAttributes = devices.stream().mapToInt(d -> d.getAttributes().size()).sum();

        // --- build writer chain ---
        InMemoryWriter inMemory = new InMemoryWriter(totalAttributes);

        List<RecordWriter> writers = new ArrayList<>();
        writers.add(inMemory);
        // Future writers (ERPNextWriter, ElasticsearchWriter, …) are added here
        // based on configuration elements, e.g.:
        //   if (config.getFrappe() != null) writers.add(new ERPNextWriter(config.getFrappe()));

        WriterDispatcher dispatcher = new WriterDispatcher(writers);

        // --- build and start engine ---
        EngineFactory factory = new EngineFactory(devices, dispatcher);
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
                logger.error("Error closing writers: {}", e.getMessage(), e);
            }
            logger.info("Shutdown complete");
        }));
    }
}
