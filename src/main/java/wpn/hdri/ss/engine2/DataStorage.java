package wpn.hdri.ss.engine2;

import wpn.hdri.ss.data2.AllRecords;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.data2.Snapshot;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 09.11.2015
 */
public class DataStorage {
    private final int totalNumberOfAttributes;

    private final Snapshot snapshot;
    private final AllRecords allRecords;

    public DataStorage(int totalNumberOfAttributes) {
        this.totalNumberOfAttributes = totalNumberOfAttributes;
        this.snapshot = new Snapshot(totalNumberOfAttributes);
        this.allRecords = new AllRecords(totalNumberOfAttributes);
    }

    public void writeRecord(SingleRecord<?> record){
        snapshot.update(record);
    }

    public void appendRecord(SingleRecord<?> record){
        snapshot.update(record);
        allRecords.add(record);
    }

    public Iterable<SingleRecord<?>> getSnapshot(){
        return snapshot;
    }

    public AllRecords getAllRecords(){
        return allRecords;
    }

    public void clear() {
        allRecords.clear();
    }

    public int getNumberOfAttributues() {
        return totalNumberOfAttributes;
    }
}
