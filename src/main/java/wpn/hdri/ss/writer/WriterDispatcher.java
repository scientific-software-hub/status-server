package wpn.hdri.ss.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.event.EventSink;

import java.util.List;

/**
 * Fans out each telemetry record to all configured sinks.
 * A failure in one sink is logged and isolated — it does not affect the others.
 */
public class WriterDispatcher implements EventSink<SingleRecord<?>>, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(WriterDispatcher.class);

    private final List<EventSink<SingleRecord<?>>> sinks;

    public WriterDispatcher(List<EventSink<SingleRecord<?>>> sinks) {
        this.sinks = List.copyOf(sinks);
    }

    @Override
    public void onEvent(SingleRecord<?> record) {
        for (EventSink<SingleRecord<?>> sink : sinks) {
            try {
                sink.onEvent(record);
            } catch (Exception e) {
                logger.error("Sink '{}' failed on record id={}: {}", sink.name(), record.id, e.getMessage(), e);
            }
        }
    }

    @Override
    public void close() throws Exception {
        for (EventSink<SingleRecord<?>> sink : sinks) {
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
        return "Dispatcher";
    }
}
