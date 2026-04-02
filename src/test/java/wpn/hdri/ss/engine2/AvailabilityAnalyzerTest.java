package wpn.hdri.ss.engine2;

import org.junit.Before;
import org.junit.Test;
import wpn.hdri.ss.event.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class AvailabilityAnalyzerTest {

    private static final int STALE_AFTER = 3;
    private static final int DOWN_AFTER = 6;
    private static final int ATTR_ID = 42;

    private List<DomainEvent> emitted;
    private AvailabilityAnalyzer analyzer;

    @Before
    public void setUp() {
        emitted = new ArrayList<>();
        analyzer = new AvailabilityAnalyzer(STALE_AFTER, DOWN_AFTER, emitted::add);
    }

    // --- helpers ---

    private void fail(int times) {
        for (int i = 0; i < times; i++)
            analyzer.onEvent(new ReadFailure(ATTR_ID, Instant.now(), "err"));
    }

    private void succeed() {
        analyzer.onEvent(new ReadSuccess(ATTR_ID, Instant.now()));
    }

    // --- tests ---

    @Test
    public void staysUpOnSingleFailure() {
        fail(1);
        assertTrue(emitted.isEmpty());
    }

    @Test
    public void transitionsToStaleAfterThreshold() {
        fail(STALE_AFTER);

        assertEquals(1, emitted.size());
        AvailabilityTransitioned t = (AvailabilityTransitioned) emitted.get(0);
        assertEquals(AvailabilityState.UP, t.from());
        assertEquals(AvailabilityState.STALE, t.to());
    }

    @Test
    public void noAdditionalEventsBeyondStaleUntilDownThreshold() {
        fail(STALE_AFTER + 1); // one beyond stale, not yet down
        assertEquals(1, emitted.size()); // still only the STALE transition
    }

    @Test
    public void transitionsToDownAndOpensDowntime() {
        fail(DOWN_AFTER);

        assertEquals(3, emitted.size());
        assertTransition(emitted.get(0), AvailabilityState.UP, AvailabilityState.STALE);
        assertTransition(emitted.get(1), AvailabilityState.STALE, AvailabilityState.DOWN);
        assertInstanceOf(DowntimeOpened.class, emitted.get(2));
    }

    @Test
    public void recoveryFromStaleEmitsTransitionOnly() {
        fail(STALE_AFTER);
        emitted.clear();

        succeed();

        assertEquals(1, emitted.size());
        assertTransition(emitted.get(0), AvailabilityState.STALE, AvailabilityState.UP);
    }

    @Test
    public void recoveryFromDownClosesDowntime() {
        fail(DOWN_AFTER);
        emitted.clear();

        succeed();

        assertEquals(2, emitted.size());
        assertTransition(emitted.get(0), AvailabilityState.DOWN, AvailabilityState.UP);
        assertInstanceOf(DowntimeClosed.class, emitted.get(1));
    }

    @Test
    public void downtimeClosedDurationIsPositive() {
        fail(DOWN_AFTER);
        succeed();

        DowntimeClosed closed = emitted.stream()
                .filter(e -> e instanceof DowntimeClosed)
                .map(e -> (DowntimeClosed) e)
                .findFirst().orElseThrow();

        assertFalse(closed.duration().isNegative());
    }

    @Test
    public void successWhenAlreadyUpEmitsNothing() {
        succeed();
        succeed();
        assertTrue(emitted.isEmpty());
    }

    @Test
    public void failureCountResetAfterRecovery() {
        fail(STALE_AFTER - 1); // just below stale — no event
        succeed();              // recover, reset counter
        emitted.clear();

        fail(STALE_AFTER - 1); // same number — should still be silent
        assertTrue(emitted.isEmpty());
    }

    @Test
    public void timeoutCountsAsFailure() {
        for (int i = 0; i < STALE_AFTER; i++)
            analyzer.onEvent(new Timeout(ATTR_ID, Instant.now()));

        assertEquals(1, emitted.size());
        assertTransition(emitted.get(0), AvailabilityState.UP, AvailabilityState.STALE);
    }

    @Test
    public void multipleAttributesTrackedIndependently() {
        int attrA = 1, attrB = 2;
        for (int i = 0; i < DOWN_AFTER; i++)
            analyzer.onEvent(new ReadFailure(attrA, Instant.now(), "err"));
        // attrB still silent
        long bEvents = emitted.stream().filter(e -> e.attributeId() == attrB).count();
        assertEquals(0, bEvents);
        // attrA has all its events
        long aEvents = emitted.stream().filter(e -> e.attributeId() == attrA).count();
        assertEquals(3, aEvents);
    }

    // --- recovery tests ---

    @Test
    public void seedDownStateClosesDowntimeOnRecovery() {
        Instant downtimeStart = Instant.now().minusSeconds(300);
        analyzer.seed(ATTR_ID, AvailabilityState.DOWN, downtimeStart);

        succeed();

        assertEquals(2, emitted.size());
        assertTransition(emitted.get(0), AvailabilityState.DOWN, AvailabilityState.UP);
        DowntimeClosed closed = (DowntimeClosed) emitted.get(1);
        assertEquals(downtimeStart, closed.openedAt());
    }

    @Test
    public void seedStaleStateTransitionsToUpOnRecovery() {
        analyzer.seed(ATTR_ID, AvailabilityState.STALE, Instant.now().minusSeconds(60));

        succeed();

        assertEquals(1, emitted.size());
        assertTransition(emitted.get(0), AvailabilityState.STALE, AvailabilityState.UP);
    }

    @Test
    public void seedUpStateEmitsNothingOnSuccess() {
        analyzer.seed(ATTR_ID, AvailabilityState.UP, Instant.now());
        succeed();
        assertTrue(emitted.isEmpty());
    }

    @Test
    public void seedDownStateContinuesAccumulatingFailures() {
        analyzer.seed(ATTR_ID, AvailabilityState.DOWN, Instant.now().minusSeconds(60));
        // more failures while DOWN should not emit anything new
        fail(3);
        assertTrue(emitted.isEmpty());
    }

    // --- assertion helpers ---

    private static void assertTransition(DomainEvent event, AvailabilityState from, AvailabilityState to) {
        assertInstanceOf(AvailabilityTransitioned.class, event);
        AvailabilityTransitioned t = (AvailabilityTransitioned) event;
        assertEquals(from, t.from());
        assertEquals(to, t.to());
    }

    private static void assertInstanceOf(Class<?> clazz, Object obj) {
        assertTrue("Expected " + clazz.getSimpleName() + " but was " + obj.getClass().getSimpleName(),
                clazz.isInstance(obj));
    }
}
