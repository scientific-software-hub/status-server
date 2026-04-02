package wpn.hdri.ss;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.configuration.Device;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.engine2.AvailabilityAnalyzer;
import wpn.hdri.ss.engine2.Engine;
import wpn.hdri.ss.engine2.EngineFactory;
import wpn.hdri.ss.event.DomainEvent;
import wpn.hdri.ss.event.EventSink;
import wpn.hdri.ss.http.MetricsServer;
import wpn.hdri.ss.source.DeviceSource;
import wpn.hdri.ss.source.XmlDeviceSource;
import wpn.hdri.ss.writer.EventDispatcher;
import wpn.hdri.ss.writer.InMemoryWriter;
import wpn.hdri.ss.writer.MariaDbSink;

import java.util.ArrayList;
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

        // --- device source ---
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

        // --- telemetry sink chain ---
        InMemoryWriter inMemory = new InMemoryWriter(totalAttributes);
        EventDispatcher<SingleRecord<?>> telemetryDispatcher = new EventDispatcher<>(List.of(inMemory));

        // --- domain event sink chain ---
        List<EventSink<DomainEvent>> domainSinks = new ArrayList<>();
        domainSinks.add(event -> logger.info("Domain event: {}", event));

        MariaDbSink mariaDbSink = null;
        if (config.getMariaDb() != null) {
            mariaDbSink = new MariaDbSink(config.getMariaDb());
            domainSinks.add(mariaDbSink);
            logger.info("MariaDB sink enabled ({})", config.getMariaDb().jdbcUrl());
        }
        EventDispatcher<DomainEvent> domainDispatcher = new EventDispatcher<>(domainSinks);

        // --- engine ---
        AvailabilityAnalyzer analyzer = new AvailabilityAnalyzer(
                config.getStaleAfter(), config.getDownAfter(), domainDispatcher);
        EngineFactory factory = new EngineFactory(devices, telemetryDispatcher, analyzer);
        Engine engine = factory.newEngine();

        if (!factory.getPendingAttributes().isEmpty()) {
            logger.warn("{} attribute(s) unavailable at startup, will retry every 30 s",
                    factory.getPendingAttributes().size());
        }

        if (mariaDbSink != null) {
            final MariaDbSink sink = mariaDbSink;

            // Register attribute names for human-readable DB records (including pending ones)
            engine.getAttributes().forEach(attr -> sink.registerAttribute(attr.id, attr.fullName));
            factory.getPendingAttributes().forEach(p -> sink.registerAttribute(p.id(), p.fullName()));

            // Restore persisted availability state before the engine starts collecting
            try {
                sink.loadCurrentStates().forEach((id, cs) ->
                        analyzer.seed(id, cs.state(), cs.since()));
                logger.info("Availability state restored from MariaDB");
            } catch (Exception e) {
                logger.warn("Could not restore state from MariaDB, starting fresh: {}", e.getMessage());
            }
        }

        // --- HTTP server ---
        MetricsServer httpServer = new MetricsServer(httpPort, inMemory);
        httpServer.start();

        engine.start();
        httpServer.markReady();
        logger.info("Engine started, ready to collect");

        // --- graceful shutdown ---
        final MariaDbSink finalMariaDbSink = mariaDbSink;
        Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(() -> {
            logger.info("Shutting down...");
            engine.stop();
            httpServer.stop();
            try { telemetryDispatcher.close(); } catch (Exception e) {
                logger.error("Error closing telemetry dispatcher: {}", e.getMessage(), e);
            }
            try { domainDispatcher.close(); } catch (Exception e) {
                logger.error("Error closing domain dispatcher: {}", e.getMessage(), e);
            }
            logger.info("Shutdown complete");
        }));
    }
}
