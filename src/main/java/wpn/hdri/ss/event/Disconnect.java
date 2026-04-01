package wpn.hdri.ss.event;

import java.time.Instant;

/**
 * Emitted when a connection to a device is lost.
 * Currently reserved for emission from the client layer (TangoClient / TineClient).
 */
public record Disconnect(int attributeId, Instant timestamp) implements TechnicalEvent {}
