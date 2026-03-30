package wpn.hdri.ss.writer;

import wpn.hdri.ss.data2.SingleRecord;

/**
 * Consumes records produced by the collection engine.
 * Implementations may write to in-memory storage, a remote database, an HTTP API, etc.
 * Multiple writers can be composed via {@link WriterDispatcher}.
 */
public interface RecordWriter extends AutoCloseable {

    /**
     * @param record the collected data point
     * @param append true  = keep full history (heavy-duty mode)
     *               false = overwrite snapshot only (light polling mode)
     */
    void write(SingleRecord<?> record, boolean append);

    default String name() {
        return getClass().getSimpleName();
    }

    @Override
    default void close() throws Exception {
    }
}
