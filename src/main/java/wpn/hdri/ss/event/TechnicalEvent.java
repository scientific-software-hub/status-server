package wpn.hdri.ss.event;

/**
 * A low-level signal emitted by a collection task after each read attempt
 * or connection-state change.  The {@link wpn.hdri.ss.engine2.AvailabilityAnalyzer}
 * consumes these events and produces domain-level availability transitions.
 */
public sealed interface TechnicalEvent extends Event
        permits ReadSuccess, ReadFailure, Timeout, Disconnect, Reconnect,
                ConnectionRefused, DeviceNotExported, DevError {
}
