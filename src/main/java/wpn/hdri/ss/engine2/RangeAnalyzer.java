package wpn.hdri.ss.engine2;

import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.event.AboveMaxClosed;
import wpn.hdri.ss.event.AboveMaxOpened;
import wpn.hdri.ss.event.BelowMinClosed;
import wpn.hdri.ss.event.BelowMinOpened;
import wpn.hdri.ss.event.DomainEvent;
import wpn.hdri.ss.event.EventSink;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Watches telemetry for any attribute that has a configured {@code min} and/or {@code max}
 * bound and emits BelowMin/AboveMax domain events when its value crosses below/back above
 * {@code min}, or above/back below {@code max}.
 *
 * <p>Edge-triggered: only fires on the crossing, not on every reading while out of range —
 * mirroring how {@link AttributeAvailability} only fires on state transitions.
 *
 * <p>Bounds are looked up by attribute name (the same name used in the XML config), so two
 * attributes sharing a name across devices share the same bounds. Breach state is tracked
 * per attribute id in a {@link ConcurrentHashMap} since different attributes are driven by
 * different polling/event tasks running concurrently.
 */
public class RangeAnalyzer implements EventSink<SingleRecord<?>> {

    /** Optional lower/upper bound for a single attribute, as configured in XML. */
    public record Bounds(Double min, Double max) {}

    private static final class State {
        boolean belowMin = false;
        boolean aboveMax = false;
        Instant belowMinSince = null;
        Instant aboveMaxSince = null;
    }

    private final Map<String, Bounds> boundsByAttributeName;
    private final EventSink<DomainEvent> domainSink;
    private final ConcurrentHashMap<Integer, State> states = new ConcurrentHashMap<>();

    public RangeAnalyzer(Map<String, Bounds> boundsByAttributeName, EventSink<DomainEvent> domainSink) {
        this.boundsByAttributeName = boundsByAttributeName;
        this.domainSink = domainSink;
    }

    @Override
    public void onEvent(SingleRecord<?> record) {
        Attribute<?> attr = record.attribute;
        if (attr == null) return;

        Bounds bounds = boundsByAttributeName.get(attr.name);
        if (bounds == null) return;
        if (!(record.value instanceof Number number)) return;

        double value = number.doubleValue();
        Instant ts = record.timestamp();
        State state = states.computeIfAbsent(attr.id, id -> new State());

        if (bounds.min() != null) {
            if (value < bounds.min() && !state.belowMin) {
                state.belowMin = true;
                state.belowMinSince = ts;
                domainSink.onEvent(new BelowMinOpened(attr.id, value, ts));
            } else if (value >= bounds.min() && state.belowMin) {
                state.belowMin = false;
                domainSink.onEvent(new BelowMinClosed(attr.id, state.belowMinSince, ts));
                state.belowMinSince = null;
            }
        }

        if (bounds.max() != null) {
            if (value > bounds.max() && !state.aboveMax) {
                state.aboveMax = true;
                state.aboveMaxSince = ts;
                domainSink.onEvent(new AboveMaxOpened(attr.id, value, ts));
            } else if (value <= bounds.max() && state.aboveMax) {
                state.aboveMax = false;
                domainSink.onEvent(new AboveMaxClosed(attr.id, state.aboveMaxSince, ts));
                state.aboveMaxSince = null;
            }
        }
    }

    @Override
    public String name() {
        return "RangeAnalyzer";
    }
}
