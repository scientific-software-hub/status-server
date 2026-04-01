package wpn.hdri.ss.event;

import java.time.Instant;

/** Emitted when a read attempt timed out waiting for a device response. */
public record Timeout(int attributeId, Instant timestamp) implements TechnicalEvent {}
