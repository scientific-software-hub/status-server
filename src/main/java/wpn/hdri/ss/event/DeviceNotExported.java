package wpn.hdri.ss.event;

import java.time.Instant;

/**
 * The Tango device server process is reachable but the device has not been exported
 * (API_DeviceNotExported). The device is not running or has not started yet.
 */
public record DeviceNotExported(int attributeId, Instant timestamp) implements TechnicalEvent {}
