package wpn.hdri.ss.engine2;

import wpn.hdri.ss.event.*;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the latest failure type for each attribute so the metrics endpoint
 * can expose it as a Prometheus label on {@code _up=0} series.
 *
 * <p>Cleared per-attribute on {@link ReadSuccess} or {@link Reconnect}.
 */
public class FailureTypeStore implements EventSink<TechnicalEvent> {

    public record FailureInfo(String type, String detail) {
        /** No detail — used for all failure types except DevError. */
        static FailureInfo of(String type) {
            return new FailureInfo(type, null);
        }
    }

    private final ConcurrentHashMap<Integer, FailureInfo> latest = new ConcurrentHashMap<>();

    @Override
    public void onEvent(TechnicalEvent event) {
        switch (event) {
            case ReadSuccess s       -> latest.remove(s.attributeId());
            case Reconnect r         -> latest.remove(r.attributeId());
            case Timeout t           -> latest.put(t.attributeId(), FailureInfo.of("Timeout"));
            case ConnectionRefused c -> latest.put(c.attributeId(), FailureInfo.of("ConnectionRefused"));
            case DeviceNotExported d -> latest.put(d.attributeId(), FailureInfo.of("DeviceNotExported"));
            case DevError e          -> latest.put(e.attributeId(), new FailureInfo("DevError", e.reason()));
            case ReadFailure f       -> latest.put(f.attributeId(), FailureInfo.of("ReadFailure"));
            case Disconnect d        -> latest.put(d.attributeId(), FailureInfo.of("Disconnect"));
        }
    }

    /** Returns null if the attribute is currently healthy. */
    public FailureInfo get(int attributeId) {
        return latest.get(attributeId);
    }

    @Override
    public String name() {
        return "FailureTypeStore";
    }
}
