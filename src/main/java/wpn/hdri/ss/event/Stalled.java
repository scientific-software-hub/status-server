package wpn.hdri.ss.event;

import java.time.Instant;

/**
 * Emitted by the engine's stall watchdog when a polled attribute's snapshot record has not been
 * refreshed for longer than its staleness threshold, despite the attribute still holding a
 * previously-read (non-null) value. Distinct from {@link ReadFailure}/{@link Timeout}: those are
 * emitted by the poll task itself after a failed read attempt; {@code Stalled} is emitted by the
 * watchdog when no read attempt appears to be happening at all (a silently-dead or wedged task).
 */
public record Stalled(int attributeId, Instant timestamp, long ageMillis) implements TechnicalEvent {}
