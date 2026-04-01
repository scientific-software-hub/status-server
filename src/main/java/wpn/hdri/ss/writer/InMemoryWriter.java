package wpn.hdri.ss.writer;

import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.engine2.DataStorage;
import wpn.hdri.ss.event.EventSink;

/**
 * Keeps the latest snapshot and (optionally) full history in memory.
 * Used for testing, small standalone deployments, and serving the /metrics endpoint.
 *
 * <p>{@code append=true}  — every record is added to the full history as well as the snapshot.
 * <p>{@code append=false} — only the snapshot is updated (light-polling / low-memory mode).
 *
 * <p>Null-value records (emitted on read failure) are always snapshot-only regardless of mode,
 * so error markers never pollute the history.
 */
public class InMemoryWriter implements EventSink<SingleRecord<?>> {

    private final DataStorage storage;
    private final boolean append;

    public InMemoryWriter(int totalAttributes, boolean append) {
        this.storage = new DataStorage(totalAttributes);
        this.append = append;
    }

    @Override
    public void onEvent(SingleRecord<?> record) {
        if (record.value == null || !append) {
            storage.writeRecord(record);
        } else {
            storage.appendRecord(record);
        }
    }

    public DataStorage getStorage() {
        return storage;
    }

    @Override
    public String name() {
        return "InMemory";
    }
}
