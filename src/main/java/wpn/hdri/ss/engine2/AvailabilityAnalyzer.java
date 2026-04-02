package wpn.hdri.ss.engine2;

import wpn.hdri.ss.event.AvailabilityState;
import wpn.hdri.ss.event.DomainEvent;
import wpn.hdri.ss.event.EventSink;
import wpn.hdri.ss.event.TechnicalEvent;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumes low-level technical events and maintains per-attribute availability state.
 * Emits domain events (AvailabilityTransitioned, DowntimeOpened, DowntimeClosed)
 * to the configured domain sink.
 *
 * <p>Thread-safe: attribute state machines are created lazily and are each
 * accessed only by the single task that owns the attribute.
 */
public class AvailabilityAnalyzer implements EventSink<TechnicalEvent> {

    private final int staleAfter;
    private final int downAfter;
    private final EventSink<DomainEvent> domainSink;

    private final ConcurrentHashMap<Integer, AttributeAvailability> states = new ConcurrentHashMap<>();

    public AvailabilityAnalyzer(int staleAfter, int downAfter, EventSink<DomainEvent> domainSink) {
        this.staleAfter = staleAfter;
        this.downAfter = downAfter;
        this.domainSink = domainSink;
    }

    @Override
    public void onEvent(TechnicalEvent event) {
        states.computeIfAbsent(event.attributeId(),
                        id -> new AttributeAvailability(id, staleAfter, downAfter, domainSink))
                .process(event);
    }

    /**
     * Seeds a single attribute's state machine from persisted storage.
     * Must be called before {@code engine.start()} so no live events race with the restore.
     */
    public void seed(int attributeId, AvailabilityState state, Instant since) {
        states.computeIfAbsent(attributeId,
                        id -> new AttributeAvailability(id, staleAfter, downAfter, domainSink))
                .restore(state, since);
    }

    @Override
    public String name() {
        return "AvailabilityAnalyzer";
    }
}
