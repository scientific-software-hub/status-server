package wpn.hdri.ss.event;

import java.time.Instant;

/**
 * The host or port was unreachable — the TCP connection could not be established.
 * Distinct from {@link Timeout} (server alive but slow) and
 * {@link DeviceNotExported} (process running, device not started).
 */
public record ConnectionRefused(int attributeId, Instant timestamp) implements TechnicalEvent {}
