package wpn.hdri.ss.event;

import java.time.Instant;
import java.time.Duration;

/** Emitted when a DOWN attribute recovers — closes the billable downtime interval. */
public record DowntimeClosed(
        int attributeId,
        Instant openedAt,
        Instant timestamp
) implements DomainEvent {

    public Duration duration() {
        return Duration.between(openedAt, timestamp);
    }
}
