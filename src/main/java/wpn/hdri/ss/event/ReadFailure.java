package wpn.hdri.ss.event;

import java.time.Instant;

/** Emitted when a read attempt failed for a non-timeout reason. */
public record ReadFailure(int attributeId, Instant timestamp, String reason) implements TechnicalEvent {}
