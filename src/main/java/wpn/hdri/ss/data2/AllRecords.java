package wpn.hdri.ss.data2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 09.11.2015
 */
public class AllRecords {
    private final CopyOnWriteArrayList<SingleRecord> data = new CopyOnWriteArrayList<>();

    public void add(SingleRecord record){
        data.add(record);
    }

    /**
     *
     * @param rTimestamp a read timestamp
     * @return all records that were added after specified timestamp
     */
    public Iterable<SingleRecord> get(long rTimestamp){
        if(data.size() == 0) return Collections.EMPTY_LIST;

        List<SingleRecord> records = Arrays.asList((SingleRecord[]) data.toArray());
        int size = records.size();
        if(rTimestamp <= records.get(0).r_t){
            return records;
        } else {
            //TODO binary search?
            int startNdx = size;
            do{
                startNdx--;
            }while(rTimestamp > records.get(startNdx).r_t);

            return records.subList(startNdx, size - 1);
        }
    }

    /**
     *
     */
    public Iterable<SingleRecord> getRange(long t0, long t1){
        if(t1 <= t0 ) throw  new IllegalArgumentException(String.format("Invalid timestamps range: %d, %d", t0, t1));
        return null;//TODO
    }
}
