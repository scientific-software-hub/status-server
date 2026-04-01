package wpn.hdri.ss.event;

import java.time.Instant;

/** Emitted on every availability state change. */
public record AvailabilityTransitioned(
        int attributeId,
        AvailabilityState from,
        AvailabilityState to,
        Instant timestamp
) implements DomainEvent {}
