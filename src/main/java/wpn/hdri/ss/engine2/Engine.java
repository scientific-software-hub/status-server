package wpn.hdri.ss.engine2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.client.ClientException;
import wpn.hdri.ss.client2.ClientAdaptor;
import wpn.hdri.ss.configuration.DeviceAttribute;
import wpn.hdri.ss.data.Method;
import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.data2.Interpolation;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.event.ReadFailure;
import wpn.hdri.ss.event.EventSink;
import wpn.hdri.ss.event.TechnicalEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 10.11.2015
 */
public class Engine {
    private final static Logger logger = LoggerFactory.getLogger(Engine.class);

    private static final long RETRY_INTERVAL_SECONDS = 30;

    public final ScheduledExecutorService exec;

    private final EventSink<SingleRecord<?>> telemetrySink;
    private final EventSink<TechnicalEvent> technicalSink;

    private final Map<String, Attribute<?>> attributesByName = new HashMap<>();

    private final List<Attribute> polledAttributes;
    private final List<Attribute> eventDrivenAttributes;

    /** Attributes that could not connect at startup — retried every RETRY_INTERVAL_SECONDS. */
    private final List<PendingAttribute> pendingAttributes;

    /** EventTasks whose subscription failed — re-subscribed every RETRY_INTERVAL_SECONDS. */
    private final ConcurrentLinkedQueue<EventTask> failedSubscriptions = new ConcurrentLinkedQueue<>();

    /** All currently-subscribed event attributes, tracked for clean stop(). */
    private final ConcurrentLinkedQueue<Attribute> subscribedEventAttrs = new ConcurrentLinkedQueue<>();

    private final Map<String, ScheduledFuture<?>> runningTasks = new HashMap<>();
    private ScheduledFuture<?> retryTask;

    public Engine(ScheduledExecutorService exec,
                  EventSink<SingleRecord<?>> telemetrySink,
                  List<Attribute> polledAttributes,
                  List<Attribute> eventDrivenAttributes,
                  EventSink<TechnicalEvent> technicalSink,
                  List<PendingAttribute> pendingAttributes) {
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
        this.pendingAttributes = pendingAttributes;
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
            subscribeWithRetry(attr);
        }
    }

    public void start() {
        logger.debug("Starting...");
        seedPendingAsFailed();
        start(-1);
        retryTask = exec.scheduleWithFixedDelay(
                this::retryFailed, RETRY_INTERVAL_SECONDS, RETRY_INTERVAL_SECONDS, TimeUnit.SECONDS);
        logger.debug("Done!");
    }

    /**
     * Seeds each pending attribute's Snapshot slot with a null-value record so it appears
     * in /metrics as _up=0 immediately, rather than being invisible.
     */
    private void seedPendingAsFailed() {
        long now = System.currentTimeMillis();
        for (PendingAttribute p : pendingAttributes) {
            Attribute<Object> stub = new Attribute<>(
                    p.id(), null, 0L, Method.EventType.NONE, Object.class,
                    p.devAttr().getAlias(), p.fullName(), p.devAttr().getName(),
                    Interpolation.LAST);
            telemetrySink.onEvent(new SingleRecord<>(stub, now, 0L, null, "ReadFailure", null));
            technicalSink.onEvent(new ReadFailure(p.id(), java.time.Instant.now(),
                    "Upstream unavailable at startup"));
        }
    }

    public void stop() {
        logger.debug("Stopping...");
        if (retryTask != null) retryTask.cancel(false);
        for (Map.Entry<String, ScheduledFuture<?>> task : runningTasks.entrySet()) {
            logger.debug("Canceling polling task for {}", task.getKey());
            task.getValue().cancel(false);
        }
        for (Attribute attr : subscribedEventAttrs) {
            logger.debug("Unsubscribing from {}", attr.fullName);
            attr.devClient.unsubscribe(attr);
        }
        logger.info("Stopped!");
    }

    public Collection<Attribute<?>> getAttributes() {
        return attributesByName.values();
    }

    // --- retry logic ---

    private void retryFailed() {
        retryPendingAttributes();
        retryFailedSubscriptions();
    }

    private void retryPendingAttributes() {
        Iterator<PendingAttribute> it = pendingAttributes.iterator();
        while (it.hasNext()) {
            PendingAttribute p = it.next();
            Class<?> type;
            try {
                type = p.client().getAttributeClass(p.devAttr().getName());
            } catch (ClientException e) {
                logger.debug("Still unavailable {}: {}", p.fullName(), e.getMessage());
                continue;
            }

            it.remove();

            DeviceAttribute devAttr = p.devAttr();
            Method.EventType eventType = Method.EventType.valueOf(devAttr.getEventType().toUpperCase());
            Interpolation interpolation = Interpolation.valueOf(devAttr.getInterpolation().toUpperCase());

            Attribute attr = new Attribute<>(
                    p.id(), (ClientAdaptor) p.client(), devAttr.getDelay(),
                    eventType, type, devAttr.getAlias(),
                    p.fullName(), devAttr.getName(), interpolation);

            attributesByName.put(attr.fullName, attr);
            logger.info("Recovered attribute {}, activating", attr.fullName);

            if (devAttr.getMethod() == Method.POLL) {
                PollTask task = new PollTask(attr, telemetrySink, technicalSink);
                ScheduledFuture<?> future = exec.scheduleWithFixedDelay(
                        task, 0L, attr.delay, TimeUnit.MILLISECONDS);
                runningTasks.put(attr.fullName, future);
            } else {
                subscribeWithRetry(attr);
            }
        }
    }

    private void retryFailedSubscriptions() {
        EventTask task;
        while ((task = failedSubscriptions.poll()) != null) {
            logger.info("Re-subscribing to {}", task.getAttribute().fullName);
            subscribeWithRetry(task.getAttribute());
        }
    }

    private void subscribeWithRetry(Attribute attr) {
        EventTask task = new EventTask(attr, telemetrySink, technicalSink);
        task.setResubscribeCallback(() -> failedSubscriptions.add(task));
        subscribedEventAttrs.add(attr);
        attr.devClient.subscribe(task);
    }
}
