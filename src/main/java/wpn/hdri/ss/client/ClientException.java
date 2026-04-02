package wpn.hdri.ss.client;

/**
 * Wraps any protocol-level error from a Tango or TINE client.
 * The {@link FailureType} is set by the client at throw-site so the engine
 * can map it to a precise {@link wpn.hdri.ss.event.TechnicalEvent} subtype
 * without importing protocol-specific exception classes.
 */
public class ClientException extends Exception {

    public enum FailureType {
        /** TCP connection could not be established — host/port unreachable. */
        CONNECTION_REFUSED,
        /** Tango: API_DeviceNotExported — server process up, device not started. */
        DEVICE_NOT_EXPORTED,
        /** Device is reachable but returned a protocol-level error (DevFailed, TINE link error). */
        DEVICE_ERROR,
        /** Read timed out waiting for a response. */
        TIMEOUT,
        /** Unclassified — fall back to message-string inspection. */
        OTHER
    }

    private final FailureType failureType;

    public ClientException(String msg, Throwable cause) {
        this(msg, cause, FailureType.OTHER);
    }

    public ClientException(String msg, Throwable cause, FailureType failureType) {
        super(msg, cause);
        this.failureType = failureType;
    }

    public FailureType getFailureType() {
        return failureType;
    }
}
