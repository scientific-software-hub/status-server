package wpn.hdri.ss.data2;

import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * This class accumulates all the records collected so far.
 * <p/>
 * Backed with {@link java.util.concurrent.CopyOnWriteArrayList}.
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 09.11.2015
 */
@ThreadSafe
public class AllRecords {
    private final int totalNumberOfAttributes;

    /**
     * Aggregates all records collected so far
     */
    private final ConcurrentSkipListSet<SingleRecord<?>> data;
    /**
     * Splits all records collected so far by attribute.id (ndx)
     */
    private final ConcurrentSkipListSet<TimedSnapshot> snapshots;

    public AllRecords(int totalNumberOfAttributes) {
        this.totalNumberOfAttributes = totalNumberOfAttributes;
        data = new ConcurrentSkipListSet<>(new SingleRecordComparator());
        snapshots = new ConcurrentSkipListSet<>(new SnapshotComparator());
    }

    public void add(SingleRecord record) {
        TimedSnapshot last;
        try {
            last = snapshots.last();

            if(last.get(record.id) != null && last.get(record.id).w_t == record.w_t) return;

            last = last.copy();
        } catch (NoSuchElementException e) {
            last = new TimedSnapshot(totalNumberOfAttributes);
        }

        last.update(record);

        snapshots.add(last);
        data.add(record);
    }

    /**
     * Returns a pair of closest records for each attribute. Any of the pairs can contain same element
     *
     * @param t
     * @return a pair of closest records for each attribute
     */
    public Iterable<SingleRecord<?>> getSnapshot(long t) {
        List<SingleRecord<?>> result = new ArrayList<>();

        TimedSnapshot key = new TimedSnapshot(t);

        TimedSnapshot right = snapshots.ceiling(key);
        if(right == null)
        try {
            return snapshots.last();
        } catch (NoSuchElementException e) {
            return Collections.emptyList();
        }

        Snapshot left = right.previous;
        if(left == null) return right;

        for (int i = 0; i < totalNumberOfAttributes; ++i) {
            SingleRecord<?> leftRecord = left.get(i);
            SingleRecord<?> rightRecord = right.get(i);

            if(leftRecord == null && rightRecord == null) continue;
            if(leftRecord == null) {
                result.add(rightRecord);
                continue;
            }
            if(rightRecord == null){
                result.add(leftRecord);
                continue;
            }

            Interpolation interpolation = leftRecord.attribute.interpolation;
            switch (interpolation) {
                case LAST:
                    result.add(leftRecord);
                    continue;
                case NEAREST:
                case LINEAR:
                    result.add(interpolation.interpolate((SingleRecord<Object>) leftRecord, (SingleRecord<Object>) rightRecord, t));
                    continue;
            }
        }

        return result;
    }

    public Iterable<? extends Snapshot> getSnapshots(long t0, long t1){
        return snapshots.subSet(new TimedSnapshot(t0), true, new TimedSnapshot(t1), true);
    }

    /**
     * @param t0 a read timestamp
     * @return all records that were added after specified timestamp, aka getRange(t0, Long.MAX_VALUE)
     */
    public Iterable<SingleRecord<?>> getRange(long t0) {
        return data.tailSet(new SingleRecord<>(null, t0, 0L, null));
    }

    /**
     * @return inclusive data range
     */
    public Iterable<SingleRecord<?>> getRange(long t0, long t1) {
        return data.subSet(new SingleRecord<>(null, t0, 0L, null), true, new SingleRecord<>(null, t1, 0L, null), true);
    }

    /**
     * Returns all the values collected so far.
     *
     * @return
     */
    public Iterable<SingleRecord<?>> getRange() {
        return data;
    }

    public void clear() {
        data.clear();
        snapshots.clear();
    }

    /**
     * Clears this records removing all data older than timestamp
     *
     * @param timestamp
     */
    public void clear(long timestamp) {
        data.headSet(new SingleRecord<>(null, timestamp, 0L, null)).clear();
        snapshots.headSet(new TimedSnapshot(timestamp)).clear();
    }

    private static class SingleRecordComparator implements Comparator<SingleRecord<?>> {

        @Override
        public int compare(SingleRecord<?> o1, SingleRecord<?> o2) {
            return Long.compare(o1.r_t, o2.r_t);
        }
    }

    private static class SnapshotComparator implements Comparator<TimedSnapshot> {

        @Override
        public int compare(TimedSnapshot o1, TimedSnapshot o2) {
            return Long.compare(o1.timestamp, o2.timestamp);
        }
    }

    private static class TimedSnapshot extends Snapshot {
        private volatile long timestamp;
        private final TimedSnapshot previous;

        public TimedSnapshot(int totalAttributesNumber) {
            super(totalAttributesNumber);
            this.previous = null;
            this.timestamp = System.currentTimeMillis();
        }

        public TimedSnapshot(long t) {
            super();
            timestamp = t;
            previous = null;
        }

        @Override
        public SingleRecord update(SingleRecord record) {
            timestamp = record.r_t;
            return super.update(record);
        }

        private TimedSnapshot(Object[] array, TimedSnapshot previous){
            super(array);
            this.timestamp = previous.timestamp;
            this.previous = previous;
        }

        public TimedSnapshot copy(){
            return new TimedSnapshot((Object[]) getArray(),this);
        }
    }
}
