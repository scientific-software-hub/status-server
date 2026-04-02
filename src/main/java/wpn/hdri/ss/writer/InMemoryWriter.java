package wpn.hdri.ss.writer;

import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.engine2.DataStorage;
import wpn.hdri.ss.event.EventSink;

/**
 * Keeps the latest snapshot in memory for serving the /metrics endpoint.
 * Null-value records (emitted on read failure) are written as-is so the
 * metrics endpoint can emit {@code _up=0} markers.
 */
public class InMemoryWriter implements EventSink<SingleRecord<?>> {

    private final DataStorage storage;

    public InMemoryWriter(int totalAttributes) {
        this.storage = new DataStorage(totalAttributes);
    }

    @Override
    public void onEvent(SingleRecord<?> record) {
        storage.writeRecord(record);
    }

    public DataStorage getStorage() {
        return storage;
    }

    @Override
    public String name() {
        return "InMemory";
    }
}
