package wpn.hdri.ss.event;

import java.time.Instant;

/**
 * Emitted when a previously disconnected device becomes reachable again.
 * Currently reserved for emission from the client layer (TangoClient / TineClient).
 */
public record Reconnect(int attributeId, Instant timestamp) implements TechnicalEvent {}
