package wpn.hdri.ss.data2;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * This class contains latest values for each attribute protocolled by the StatusServer
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 09.11.2015
 */
public class Snapshot implements Iterable<SingleRecord<?>>{
    private final AtomicReferenceArray<SingleRecord<?>> data;

    public Snapshot(int totalAttributesNumber) {
        //TODO initialize array with non-null values (SingleRecore with value == null)
        this.data = new AtomicReferenceArray<>(totalAttributesNumber);
    }

    protected Snapshot(){
        this.data = null;
    }

    protected Snapshot(Object[] array){
        if (array == null ) this.data = null;
        else this.data = new AtomicReferenceArray(array);
    }

    /**
     * By definition there is 1-to-1 relationship between indices and threads, i.e.
     * only one thread writes toa particular column, but many threads can write into
     * different columns simultaneously.
     *
     * @param record a new record
     * @return an old Record
     */
    public SingleRecord update(SingleRecord record){
        int ndx = record.id;
        return data.getAndSet(ndx, record);
    }

    /**
     * Replaces the slot's record only if it is still exactly {@code expect} (reference identity).
     * Used by the stall watchdog to mark a record stalled without racing a concurrent recovery
     * write from the attribute's own poll task.
     *
     * @return true if the slot was updated
     */
    public boolean compareAndSet(SingleRecord<?> expect, SingleRecord<?> update){
        int ndx = update.id;
        return data.compareAndSet(ndx, expect, update);
    }

    /**
     * @return Iterator over the slots, read with acquire semantics
     */
    @Override
    public final Iterator<SingleRecord<?>> iterator() {
        final int size = data.length();

        return new Iterator<SingleRecord<?>>() {
            private int pos = 0;

            @Override
            public boolean hasNext() {
                return pos < size;
            }

            @Override
            public SingleRecord next() {
                return data.getAcquire(pos++);
            }
        };
    }

    public SingleRecord<?> get(int ndx){
        return data.get(ndx);
    }
}
