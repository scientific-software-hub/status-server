package wpn.hdri.ss.writer;

import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.engine2.DataStorage;

/**
 * Keeps the latest snapshot and full history in memory.
 * Used for testing, small standalone deployments, and serving the /metrics endpoint.
 */
public class InMemoryWriter implements RecordWriter {

    private final DataStorage storage;

    public InMemoryWriter(int totalAttributes) {
        this.storage = new DataStorage(totalAttributes);
    }

    @Override
    public void write(SingleRecord<?> record, boolean append) {
        if (append)
            storage.appendRecord(record);
        else
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
