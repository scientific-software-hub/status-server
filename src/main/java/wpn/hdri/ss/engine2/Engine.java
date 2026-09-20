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
import wpn.hdri.ss.data2.Snapshot;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.event.ReadFailure;
import wpn.hdri.ss.event.EventSink;
import wpn.hdri.ss.event.Stalled;
import wpn.hdri.ss.event.TechnicalEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
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

    /** Floor for the stall threshold, so fast-polled attributes don't flap on ordinary jitter. */
    private static final long MIN_STALL_THRESHOLD_MILLIS = 60_000L;
    /** Multiple of an attribute's poll delay it may go unrefreshed before being marked Stalled. */
    private static final long STALL_THRESHOLD_DELAY_MULTIPLIER = 5;

    public final ScheduledExecutorService exec;

    private final EventSink<SingleRecord<?>> telemetrySink;
    private final EventSink<TechnicalEvent> technicalSink;
    private final DataStorage storage;

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
                  List<PendingAttribute> pendingAttributes,
                  DataStorage storage) {
        this.exec = exec;
        this.telemetrySink = telemetrySink;
        this.technicalSink = technicalSink;
        this.storage = storage;
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
        checkStalls();
    }

    // --- stall detection & repair ---

    /**
     * Detects polled attributes whose snapshot record has not been refreshed for longer than
     * their staleness threshold. Two distinct failure modes get identical symptoms — a dead
     * ScheduledFuture (task killed by an uncaught throwable, silently, per
     * ScheduledThreadPoolExecutor's contract) or a wedged one (a read blocking forever) — so both
     * are diagnosed and repaired here rather than left for a process restart.
     */
    private void checkStalls() {
        long now = System.currentTimeMillis();
        Snapshot snapshot = storage.getSnapshot();

        for (Map.Entry<String, ScheduledFuture<?>> entry : new ArrayList<>(runningTasks.entrySet())) {
            String fullName = entry.getKey();
            Attribute<?> attr = attributesByName.get(fullName);
            if (attr == null || attr.delay <= 0) continue; // not a polled attribute

            SingleRecord<?> record = snapshot.get(attr.id);
            if (record == null || record.value == null) continue; // no successful read yet, or already reported failed

            long age = now - record.r_t;
            long threshold = Math.max(STALL_THRESHOLD_DELAY_MULTIPLIER * attr.delay, MIN_STALL_THRESHOLD_MILLIS);
            if (age <= threshold) continue;

            repairDeadOrWedgedTask(fullName, attr, entry.getValue());
            markStalled(attr, record, age);
            technicalSink.onEvent(new Stalled(attr.id, Instant.now(), age));
        }
    }

    private void repairDeadOrWedgedTask(String fullName, Attribute<?> attr, ScheduledFuture<?> future) {
        if (future.isDone()) {
            // A periodic ScheduledFuture is only "done" if it was cancelled or terminated by an
            // uncaught throwable — recover that throwable so the real production root cause is logged.
            try {
                future.get();
                logger.error("{}: poll task stopped producing executions unexpectedly, rescheduling", fullName);
            } catch (ExecutionException e) {
                logger.error("{}: poll task died from an uncaught exception, rescheduling", fullName, e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("{}: interrupted while inspecting dead poll task, rescheduling anyway", fullName);
            } catch (Exception e) {
                logger.error("{}: poll task stopped ({}), rescheduling", fullName, e.toString());
            }
        } else {
            logger.error("{}: poll task appears stuck reading, interrupting and rescheduling", fullName);
            future.cancel(true);
        }

        PollTask task = new PollTask(attr, telemetrySink, technicalSink);
        ScheduledFuture<?> replacement = exec.scheduleWithFixedDelay(
                task, 0L, attr.delay, TimeUnit.MILLISECONDS);
        runningTasks.put(fullName, replacement);
    }

    /** Captures the wildcard so a properly-typed replacement {@link SingleRecord} can be built. */
    private <T> void markStalled(Attribute<T> attr, SingleRecord<?> record, long age) {
        @SuppressWarnings("unchecked")
        T value = (T) record.value;
        SingleRecord<T> stalledRecord = new SingleRecord<>(
                attr, record.r_t, record.w_t, value, "Stalled", "no update for " + age + " ms");
        // CAS against the exact instance inspected — if a repaired task already wrote a fresh
        // record concurrently, don't clobber it with a stale marker.
        storage.getSnapshot().compareAndSet(record, stalledRecord);
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
