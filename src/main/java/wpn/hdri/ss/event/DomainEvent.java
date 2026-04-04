package wpn.hdri.ss.event;

/**
 * A business-level event produced by the {@link wpn.hdri.ss.engine2.AvailabilityAnalyzer}.
 * Domain events carry meaning for billing, SLA, and audit — unlike raw technical signals.
 */
public sealed interface DomainEvent extends Event
        permits AvailabilityTransitioned, DowntimeOpened, DowntimeClosed {
}
