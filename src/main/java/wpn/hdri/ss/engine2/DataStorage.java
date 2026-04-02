package wpn.hdri.ss.engine2;

import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.data2.Snapshot;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 09.11.2015
 */
public class DataStorage {

    private final Snapshot snapshot;

    public DataStorage(int totalNumberOfAttributes) {
        this.snapshot = new Snapshot(totalNumberOfAttributes);
    }

    public void writeRecord(SingleRecord<?> record){
        snapshot.update(record);
    }

    public Snapshot getSnapshot(){
        return snapshot;
    }
}
