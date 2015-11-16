package wpn.hdri.ss.data2;

import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * This class accumulates all the records collected so far.
 *
 * Backed with {@link java.util.concurrent.CopyOnWriteArrayList}.
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 09.11.2015
 */
@ThreadSafe
public class AllRecords {
    private final int totalNumberOfAttributes;

    /**
     *
     * Aggregates all records collected so far
     */
    private final ConcurrentSkipListSet<SingleRecord<?>> data = new ConcurrentSkipListSet<>(new SingleRecordComparator());
    /**
     *
     * Splits all records collected so far by attribute.id (ndx)
     */
    private final ConcurrentSkipListSet<SingleRecord<?>>[] recordsByNdx;

    public AllRecords(int totalNumberOfAttributes) {
        this.totalNumberOfAttributes = totalNumberOfAttributes;
        recordsByNdx = new ConcurrentSkipListSet[totalNumberOfAttributes];
        for(int i = 0; i < totalNumberOfAttributes; ++i){
            recordsByNdx[i] = new ConcurrentSkipListSet<>(new SingleRecordComparator());
        }
    }

    public void add(SingleRecord record){
        data.add(record);
        recordsByNdx[record.attribute.id].add(record);
    }

    /**
     *  Returns a pair of closest records for each attribute. Any of the pairs can contain same element
     *
     * @param t
     * @return a pair of closest records for each attribute
     */
    public Iterable<Map.Entry<SingleRecord<?>,SingleRecord<?>>> getSnapshot(int[] indices, long t){
        List<Map.Entry<SingleRecord<?>,SingleRecord<?>>> result = new ArrayList<>();

        SingleRecord<Object> key = new SingleRecord<>(null, t, 0L, null);
        for(int i = 0; i < indices.length; ++ i){
            ConcurrentSkipListSet<SingleRecord<?>> records = recordsByNdx[indices[i]];
            result.add(new AbstractMap.SimpleEntry<SingleRecord<?>, SingleRecord<?>>(records.floor(key), records.ceiling(key)));
        }

        return result;
    }

    /**
     *
     * @param t0 a read timestamp
     * @return all records that were added after specified timestamp, aka getRange(t0, Long.MAX_VALUE)
     */
    public Iterable<SingleRecord<?>> getRange(long t0){
        return data.tailSet(new SingleRecord<>(null, t0, 0L, null));
    }

    /**
     *
     */
    public Iterable<SingleRecord<?>> getRange(long t0, long t1){
        return data.subSet(new SingleRecord<>(null, t0, 0L, null), new SingleRecord<>(null, t1, 0L, null));
    }

    private static class SingleRecordComparator implements Comparator<SingleRecord<?>> {

        @Override
        public int compare(SingleRecord<?> o1, SingleRecord<?> o2) {
            return Long.compare(o1.r_t, o2.r_t);
        }
    }
}
