package wpn.hdri.ss.event;

/**
 * A generic observer that receives events of type {@code T}.
 *
 * <p>Replaces the separate {@code RecordWriter} and {@code TechnicalEventListener}
 * interfaces — both are concrete examples of an event sink over different event types:
 * <ul>
 *   <li>{@code EventSink<SingleRecord<?>>} — telemetry from the collection engine</li>
 *   <li>{@code EventSink<TechnicalEvent>}  — availability signals (ReadSuccess, ReadFailure, …)</li>
 *   <li>{@code EventSink<DomainEvent>}     — future: DowntimeOpened, DowntimeClosed, …</li>
 * </ul>
 *
 * <p>Implementations that hold resources should additionally implement {@link AutoCloseable}.
 */
@FunctionalInterface
public interface EventSink<T> {

    void onEvent(T event);

    /** Human-readable name used in log messages. Override for meaningful output. */
    default String name() {
        return getClass().getSimpleName();
    }
}
