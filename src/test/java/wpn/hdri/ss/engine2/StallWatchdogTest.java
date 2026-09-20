package wpn.hdri.ss.engine2;

import org.junit.Test;
import wpn.hdri.ss.client.ClientException;
import wpn.hdri.ss.client2.ClientAdaptor;
import wpn.hdri.ss.data.Method;
import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.data2.Interpolation;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.event.EventSink;
import wpn.hdri.ss.event.Stalled;
import wpn.hdri.ss.event.TechnicalEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Coverage for Engine's stall watchdog: detects a polled attribute whose snapshot record has not
 * been refreshed past its staleness threshold, marks it "Stalled" (preserving the last known
 * value/timestamp), emits a {@link Stalled} technical event, and repairs the dead/wedged
 * {@link ScheduledFuture} backing it — the fix for attributes that report a value forever after
 * their poll task silently stops running.
 *
 * <p>Uses reflection to inject a stub {@link ScheduledFuture} into {@code runningTasks} and to
 * invoke the private {@code checkStalls()} directly, so the 30s retry cadence and a real device
 * connection are not needed.
 */
public class StallWatchdogTest {

    /** Always fails; the watchdog's repaired task should not silently succeed and mask assertions. */
    private static class FailingClient implements ClientAdaptor {
        @Override
        public <T> SingleRecord<T> read(Attribute<T> attr) throws ClientException {
            throw new ClientException("test stub — no real device", new RuntimeException());
        }

        @Override
        public void subscribe(EventTask eventTask) {}

        @Override
        public void unsubscribe(Attribute<?> attr) {}
    }

    private static Attribute<Integer> attr(int id, String fullName) {
        return new Attribute<>(id, new FailingClient(), 20L, Method.EventType.NONE, Integer.class,
                "alias", fullName, "name", Interpolation.LAST);
    }

    /** Minimal ScheduledFuture stub so tests can drive checkStalls() without a live scheduled task. */
    private static class StubFuture implements ScheduledFuture<Object> {
        private final boolean done;
        final AtomicBoolean cancelled = new AtomicBoolean(false);

        StubFuture(boolean done) { this.done = done; }

        @Override public long getDelay(TimeUnit unit) { return 0; }
        @Override public int compareTo(Delayed o) { return 0; }
        @Override public boolean cancel(boolean mayInterruptIfRunning) { cancelled.set(true); return true; }
        @Override public boolean isCancelled() { return cancelled.get(); }
        @Override public boolean isDone() { return done; }
        @Override public Object get() { return null; }
        @Override public Object get(long timeout, TimeUnit unit) { return null; }
    }

    @SuppressWarnings("unchecked")
    private static void injectRunningTask(Engine engine, String fullName, ScheduledFuture<?> future) throws Exception {
        Field f = Engine.class.getDeclaredField("runningTasks");
        f.setAccessible(true);
        ((Map<String, ScheduledFuture<?>>) f.get(engine)).put(fullName, future);
    }

    private static void invokeCheckStalls(Engine engine) throws Exception {
        java.lang.reflect.Method m = Engine.class.getDeclaredMethod("checkStalls");
        m.setAccessible(true);
        m.invoke(engine);
    }

    private static Engine newEngine(Attribute<Integer> attribute, EventSink<SingleRecord<?>> telemetrySink,
                                     EventSink<TechnicalEvent> technicalSink, DataStorage storage,
                                     ScheduledExecutorService exec) {
        List<Attribute> polled = new ArrayList<>();
        polled.add(attribute);
        return new Engine(exec, telemetrySink, polled, new ArrayList<>(), technicalSink, new ArrayList<>(), storage);
    }

    @Test
    public void marksStaleRecordAndEmitsStalledEvent() throws Exception {
        Attribute<Integer> attribute = attr(0, "tango://host:10000/a/b/c");
        DataStorage storage = new DataStorage(1);
        long oldTimestamp = System.currentTimeMillis() - 120_000; // 2 min ago, past the 60s floor
        storage.writeRecord(new SingleRecord<>(attribute, oldTimestamp, oldTimestamp, 99));

        List<TechnicalEvent> technical = new CopyOnWriteArrayList<>();
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        try {
            Engine engine = newEngine(attribute, r -> {}, technical::add, storage, exec);

            // Simulates a periodic task silently killed by ScheduledThreadPoolExecutor after an
            // uncaught throwable: isDone() == true with no exception ever logged anywhere else.
            injectRunningTask(engine, attribute.fullName, new StubFuture(true));

            invokeCheckStalls(engine);

            SingleRecord<?> stalled = storage.getSnapshot().get(0);
            assertEquals("Stalled", stalled.failureType);
            assertEquals(Integer.valueOf(99), stalled.value); // last known value preserved
            assertEquals(oldTimestamp, stalled.r_t); // original read timestamp preserved, so age keeps growing

            assertEquals(1, technical.size());
            assertTrue(technical.get(0) instanceof Stalled);
            Stalled event = (Stalled) technical.get(0);
            assertEquals(0, event.attributeId());
            assertTrue(event.ageMillis() >= 120_000);
        } finally {
            exec.shutdownNow();
        }
    }

    @Test
    public void wedgedTaskIsInterruptedAndRescheduled() throws Exception {
        Attribute<Integer> attribute = attr(0, "tango://host:10000/a/b/c");
        DataStorage storage = new DataStorage(1);
        long oldTimestamp = System.currentTimeMillis() - 120_000;
        storage.writeRecord(new SingleRecord<>(attribute, oldTimestamp, oldTimestamp, 7));

        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        try {
            Engine engine = newEngine(attribute, r -> {}, e -> {}, storage, exec);

            StubFuture wedged = new StubFuture(false); // still "running" from the executor's point of view
            injectRunningTask(engine, attribute.fullName, wedged);

            invokeCheckStalls(engine);

            assertTrue("a wedged (not-done) future must be interrupted", wedged.cancelled.get());

            @SuppressWarnings("unchecked")
            Map<String, ScheduledFuture<?>> runningTasks =
                    (Map<String, ScheduledFuture<?>>) getPrivateField(engine, "runningTasks");
            assertTrue("a replacement task must be scheduled", runningTasks.get(attribute.fullName) != wedged);
        } finally {
            exec.shutdownNow();
        }
    }

    @Test
    public void freshRecordIsNotTouched() throws Exception {
        Attribute<Integer> attribute = attr(0, "tango://host:10000/a/b/c");
        DataStorage storage = new DataStorage(1);
        long now = System.currentTimeMillis();
        storage.writeRecord(new SingleRecord<>(attribute, now, now, 1));

        List<TechnicalEvent> technical = new CopyOnWriteArrayList<>();
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        try {
            Engine engine = newEngine(attribute, r -> {}, technical::add, storage, exec);
            injectRunningTask(engine, attribute.fullName, new StubFuture(false));

            invokeCheckStalls(engine);

            assertTrue(technical.isEmpty());
            assertNull(storage.getSnapshot().get(0).failureType);
        } finally {
            exec.shutdownNow();
        }
    }

    private static Object getPrivateField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }
}
