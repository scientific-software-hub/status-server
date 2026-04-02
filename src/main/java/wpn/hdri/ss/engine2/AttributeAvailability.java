package wpn.hdri.ss.engine2;

import wpn.hdri.ss.event.*;

import java.time.Instant;

/**
 * Per-attribute availability state machine.
 *
 * <pre>
 *   consecutive failures >= staleAfter  : UP    → STALE
 *   consecutive failures >= downAfter   : STALE → DOWN  (emits DowntimeOpened)
 *   any success                         : any   → UP    (emits DowntimeClosed if recovering from DOWN)
 * </pre>
 *
 * Not thread-safe by design: each attribute is driven by a single polling task.
 */
class AttributeAvailability {

    private final int attributeId;
    private final int staleAfter;
    private final int downAfter;
    private final EventSink<DomainEvent> domainSink;

    private AvailabilityState state = AvailabilityState.UP;
    private int consecutiveFailures = 0;
    private Instant downtimeStart = null;

    AttributeAvailability(int attributeId, int staleAfter, int downAfter, EventSink<DomainEvent> domainSink) {
        this.attributeId = attributeId;
        this.staleAfter = staleAfter;
        this.downAfter = downAfter;
        this.domainSink = domainSink;
    }

    void process(TechnicalEvent event) {
        switch (event) {
            case ReadSuccess s  -> handleSuccess(s.timestamp());
            case Reconnect r    -> handleSuccess(r.timestamp());
            case ReadFailure f  -> handleFailure(f.timestamp());
            case Timeout t      -> handleFailure(t.timestamp());
            case Disconnect d   -> handleFailure(d.timestamp());
        }
    }

    AvailabilityState state() {
        return state;
    }

    /**
     * Restores persisted state on startup without emitting any domain events.
     * For DOWN state, {@code since} becomes the downtimeStart so a subsequent
     * recovery correctly closes the existing interval.
     */
    void restore(AvailabilityState restoredState, Instant since) {
        this.state = restoredState;
        this.consecutiveFailures = restoredState == AvailabilityState.UP ? 0 : downAfter;
        this.downtimeStart = restoredState == AvailabilityState.DOWN ? since : null;
    }

    private void handleSuccess(Instant ts) {
        consecutiveFailures = 0;
        if (state == AvailabilityState.UP) return;

        AvailabilityState prev = state;
        state = AvailabilityState.UP;
        domainSink.onEvent(new AvailabilityTransitioned(attributeId, prev, AvailabilityState.UP, ts));

        if (prev == AvailabilityState.DOWN) {
            domainSink.onEvent(new DowntimeClosed(attributeId, downtimeStart, ts));
            downtimeStart = null;
        }
    }

    private void handleFailure(Instant ts) {
        consecutiveFailures++;

        if (state == AvailabilityState.UP && consecutiveFailures >= staleAfter) {
            state = AvailabilityState.STALE;
            domainSink.onEvent(new AvailabilityTransitioned(attributeId, AvailabilityState.UP, AvailabilityState.STALE, ts));
        } else if (state == AvailabilityState.STALE && consecutiveFailures >= downAfter) {
            state = AvailabilityState.DOWN;
            downtimeStart = ts;
            domainSink.onEvent(new AvailabilityTransitioned(attributeId, AvailabilityState.STALE, AvailabilityState.DOWN, ts));
            domainSink.onEvent(new DowntimeOpened(attributeId, ts));
        }
    }
}
