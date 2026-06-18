package wpn.hdri.ss.event;

import java.time.Instant;

/**
 * Emitted when an attribute's value crosses below its configured {@code min} —
 * opens a downtime interval.
 *
 * <p>Suggested visualization color: orange/yellow (degraded-low warning).
 */
public record BelowMinOpened(int attributeId, double value, Instant timestamp) implements DomainEvent {}
