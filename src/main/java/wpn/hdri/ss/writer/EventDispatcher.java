package wpn.hdri.ss.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.event.Event;
import wpn.hdri.ss.event.EventSink;

import java.util.List;

/**
 * Fans out events of type {@code T} to all configured sinks.
 * A failure in one sink is logged and isolated — it does not affect the others.
 *
 * <p>Replaces the typed {@code WriterDispatcher} — works for any event type:
 * telemetry ({@code SingleRecord<?>}), domain events ({@code DomainEvent}), etc.
 */
public class EventDispatcher<T extends Event> implements EventSink<T>, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(EventDispatcher.class);

    private final List<EventSink<T>> sinks;

    public EventDispatcher(List<EventSink<T>> sinks) {
        this.sinks = List.copyOf(sinks);
    }

    @Override
    public void onEvent(T event) {
        for (EventSink<T> sink : sinks) {
            try {
                sink.onEvent(event);
            } catch (Exception e) {
                logger.error("Sink '{}' failed: {}", sink.name(), e.getMessage(), e);
            }
        }
    }

    @Override
    public void close() throws Exception {
        for (EventSink<T> sink : sinks) {
            if (sink instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    logger.error("Failed to close sink '{}': {}", sink.name(), e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public String name() {
        return "EventDispatcher";
    }
}
