package wpn.hdri.ss.event;

import java.time.Instant;

/**
 * A low-level signal emitted by a collection task after each read attempt
 * or connection-state change.  The {@link wpn.hdri.ss.engine2.AvailabilityAnalyzer}
 * consumes these events and produces domain-level availability transitions.
 */
public sealed interface TechnicalEvent
        permits ReadSuccess, ReadFailure, Timeout, Disconnect, Reconnect,
                ConnectionRefused, DeviceNotExported, DevError {

    /** Internal attribute id (matches {@link wpn.hdri.ss.data2.Attribute#id}). */
    int attributeId();

    /** Wall-clock time at which the event was detected. */
    Instant timestamp();
}
