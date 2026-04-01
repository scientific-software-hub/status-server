package wpn.hdri.ss.event;

import java.time.Instant;

/** Emitted when a read or event callback completed without error. */
public record ReadSuccess(int attributeId, Instant timestamp) implements TechnicalEvent {}
