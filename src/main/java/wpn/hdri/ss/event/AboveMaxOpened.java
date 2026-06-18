package wpn.hdri.ss.event;

import java.time.Instant;

/**
 * Emitted when an attribute's value crosses above its configured {@code max} —
 * opens a downtime interval.
 *
 * <p>Suggested visualization color: red (exceeds-safe-upper-bound alarm) — the
 * opposite severity end from {@link BelowMinOpened}'s orange/yellow.
 */
public record AboveMaxOpened(int attributeId, double value, Instant timestamp) implements DomainEvent {}
