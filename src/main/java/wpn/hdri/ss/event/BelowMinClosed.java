package wpn.hdri.ss.event;

import java.time.Duration;
import java.time.Instant;

/** Emitted when a below-min attribute recovers back to/above {@code min} — closes the downtime interval. */
public record BelowMinClosed(
        int attributeId,
        Instant openedAt,
        Instant timestamp
) implements DomainEvent {

    public Duration duration() {
        return Duration.between(openedAt, timestamp);
    }
}
