package wpn.hdri.ss.event;

import java.time.Instant;

/**
 * The device server is reachable and the device is exported, but the device itself
 * reported an error (Tango DevFailed or TINE link error). The {@code reason} field
 * carries the protocol-level error description (e.g. Tango {@code DevFailed.errors[0].reason}
 * or a TINE error string).
 */
public record DevError(int attributeId, Instant timestamp, String reason) implements TechnicalEvent {}
