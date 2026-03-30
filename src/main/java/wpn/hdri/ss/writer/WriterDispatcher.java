package wpn.hdri.ss.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.data2.SingleRecord;

import java.util.List;

/**
 * Fans out each record to all configured writers.
 * A failure in one writer is logged and isolated — it does not affect the others.
 */
public class WriterDispatcher implements RecordWriter {

    private static final Logger logger = LoggerFactory.getLogger(WriterDispatcher.class);

    private final List<RecordWriter> writers;

    public WriterDispatcher(List<RecordWriter> writers) {
        this.writers = List.copyOf(writers);
    }

    @Override
    public void write(SingleRecord<?> record, boolean append) {
        for (RecordWriter writer : writers) {
            try {
                writer.write(record, append);
            } catch (Exception e) {
                logger.error("Writer '{}' failed on record id={}: {}", writer.name(), record.id, e.getMessage(), e);
            }
        }
    }

    @Override
    public void close() throws Exception {
        for (RecordWriter writer : writers) {
            try {
                writer.close();
            } catch (Exception e) {
                logger.error("Failed to close writer '{}': {}", writer.name(), e.getMessage(), e);
            }
        }
    }

    @Override
    public String name() {
        return "Dispatcher";
    }
}
