package wpn.hdri.ss.engine2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.event.EventSink;
import wpn.hdri.ss.event.TechnicalEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 10.11.2015
 */
public class Engine {
    private final static Logger logger = LoggerFactory.getLogger(Engine.class);

    public final ScheduledExecutorService exec;

    private final EventSink<SingleRecord<?>> telemetrySink;
    private final EventSink<TechnicalEvent> technicalSink;

    private final Map<String, Attribute<?>> attributesByName = new HashMap<>();

    private final List<Attribute> polledAttributes;
    private final List<Attribute> eventDrivenAttributes;

    private final Map<String, ScheduledFuture<?>> runningTasks = new HashMap<>();

    public Engine(ScheduledExecutorService exec,
                  EventSink<SingleRecord<?>> telemetrySink,
                  List<Attribute> polledAttributes,
                  List<Attribute> eventDrivenAttributes,
                  EventSink<TechnicalEvent> technicalSink) {
        this.exec = exec;
        this.telemetrySink = telemetrySink;
        this.technicalSink = technicalSink;
        this.polledAttributes = polledAttributes;
        for (Attribute<?> attr : polledAttributes) {
            attributesByName.put(attr.fullName, attr);
        }
        this.eventDrivenAttributes = eventDrivenAttributes;
        for (Attribute<?> attr : eventDrivenAttributes) {
            attributesByName.put(attr.fullName, attr);
        }
    }

    private void start(long delay) {
        for (Attribute attr : polledAttributes) {
            logger.debug("Scheduling polling task for {}", attr.fullName);
            PollTask task = new PollTask(attr, telemetrySink, technicalSink);
            runningTasks.put(attr.fullName,
                    exec.scheduleWithFixedDelay(
                            task, 0L, delay == -1 ? attr.delay : delay, TimeUnit.MILLISECONDS));
        }
        for (Attribute attr : eventDrivenAttributes) {
            logger.debug("Subscribing to {}", attr.fullName);
            attr.devClient.subscribe(new EventTask(attr, telemetrySink, technicalSink));
        }
    }

    public void start() {
        logger.debug("Starting...");
        start(-1);
        logger.debug("Done!");
    }

    public void stop() {
        logger.debug("Stopping...");
        for (Map.Entry<String, ScheduledFuture<?>> task : runningTasks.entrySet()) {
            logger.debug("Canceling polling task for {}", task.getKey());
            task.getValue().cancel(false);
        }
        for (Attribute attr : eventDrivenAttributes) {
            logger.debug("Unsubscribing from {}", attr.fullName);
            attr.devClient.unsubscribe(attr);
        }
        logger.info("Stopped!");
    }

    public Collection<Attribute<?>> getAttributes() {
        return attributesByName.values();
    }
}
