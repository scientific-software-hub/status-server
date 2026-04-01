package wpn.hdri.ss.event;

import java.time.Instant;

/**
 * A business-level event produced by the {@link wpn.hdri.ss.engine2.AvailabilityAnalyzer}.
 * Domain events carry meaning for billing, SLA, and audit — unlike raw technical signals.
 */
public sealed interface DomainEvent
        permits AvailabilityTransitioned, DowntimeOpened, DowntimeClosed {

    int attributeId();
    Instant timestamp();
}
