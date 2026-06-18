package wpn.hdri.ss.event;

import java.time.Duration;
import java.time.Instant;

/** Emitted when an above-max attribute recovers back to/below {@code max} — closes the downtime interval. */
public record AboveMaxClosed(
        int attributeId,
        Instant openedAt,
        Instant timestamp
) implements DomainEvent {

    public Duration duration() {
        return Duration.between(openedAt, timestamp);
    }
}
