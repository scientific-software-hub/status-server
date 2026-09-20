package wpn.hdri.ss.engine2;

import org.junit.Test;
import wpn.hdri.ss.client2.ClientAdaptor;
import wpn.hdri.ss.data.Method;
import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.data2.Interpolation;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.event.EventSink;
import wpn.hdri.ss.event.ReadFailure;
import wpn.hdri.ss.event.ReadSuccess;
import wpn.hdri.ss.event.TechnicalEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Regression coverage for the root cause of the "10 000 minutes stale, still reported UP" bug:
 * an unchecked exception escaping {@link PollTask#run()} used to silently cancel all future
 * executions of the {@link ScheduledFuture} — with no log line, per
 * {@link java.util.concurrent.ScheduledThreadPoolExecutor}'s documented contract.
 */
public class PollTaskTest {

    private static Attribute<Object> attr(ClientAdaptor client) {
        return new Attribute<>(0, client, 20L, Method.EventType.NONE, Object.class,
                "alias", "tango://host:10000/a/b/c", "c", Interpolation.LAST);
    }

    /** A ClientAdaptor whose read() always throws an unchecked exception, not a ClientException. */
    private static class ThrowingClient implements ClientAdaptor {
        @Override
        public <T> SingleRecord<T> read(Attribute<T> attr) {
            throw new IllegalStateException("boom: simulated org.omg.CORBA.SystemException");
        }

        @Override
        public void subscribe(EventTask eventTask) {}

        @Override
        public void unsubscribe(Attribute<?> attr) {}
    }

    /** Throws on the first read, then succeeds on every subsequent read. */
    private static class FlakyOnceClient implements ClientAdaptor {
        private final AtomicInteger calls = new AtomicInteger(0);

        @Override
        public <T> SingleRecord<T> read(Attribute<T> attr) {
            int n = calls.incrementAndGet();
            if (n == 1) {
                throw new RuntimeException("transient CORBA blip");
            }
            return new SingleRecord<>(attr, System.currentTimeMillis(), System.currentTimeMillis(), (T) Integer.valueOf(n));
        }

        @Override
        public void subscribe(EventTask eventTask) {}

        @Override
        public void unsubscribe(Attribute<?> attr) {}
    }

    @Test
    public void unhandledExceptionDoesNotPropagateAndProducesReadFailure() {
        List<SingleRecord<?>> records = new CopyOnWriteArrayList<>();
        List<TechnicalEvent> events = new CopyOnWriteArrayList<>();

        Attribute<Object> attribute = attr(new ThrowingClient());
        PollTask task = new PollTask(attribute, records::add, events::add);

        task.run(); // must not throw

        assertEquals(1, records.size());
        assertNull("failed record must carry a null value", records.get(0).value);
        assertEquals("ReadFailure", records.get(0).failureType);

        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof ReadFailure);
    }

    @Test
    public void scheduledTaskSurvivesAnUncaughtExceptionAndKeepsFiring() throws Exception {
        List<SingleRecord<?>> records = new CopyOnWriteArrayList<>();
        List<TechnicalEvent> events = new CopyOnWriteArrayList<>();
        CountDownLatch sawThreeSuccesses = new CountDownLatch(3);

        EventSink<TechnicalEvent> technicalSink = event -> {
            events.add(event);
            if (event instanceof ReadSuccess) sawThreeSuccesses.countDown();
        };

        Attribute<Object> attribute = attr(new FlakyOnceClient());
        PollTask task = new PollTask(attribute, records::add, technicalSink);

        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        try {
            ScheduledFuture<?> future = exec.scheduleWithFixedDelay(task, 0L, 10L, TimeUnit.MILLISECONDS);

            // Before the fix, the first (throwing) execution would have cancelled all future
            // ones — sawThreeSuccesses would never reach zero and this would time out.
            assertTrue("poll task should keep firing after an uncaught exception",
                    sawThreeSuccesses.await(5, TimeUnit.SECONDS));
            assertFalse("the ScheduledFuture itself must not have been silently cancelled",
                    future.isDone());

            future.cancel(true);
        } finally {
            exec.shutdownNow();
        }
    }
}
