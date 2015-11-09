package wpn.hdri.ss.data2;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;

/**
 * This class contains latest values for each attribute protocolled by the StatusServer
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 09.11.2015
 */
public class Snapshot implements Iterable<SingleRecord>{
    private final AtomicReferenceArray<SingleRecord> data;

    private static final Field array;
    static {
        try{
            array = AtomicReferenceArray.class.getDeclaredField("array");
        } catch (NoSuchFieldException e){
            throw new AssertionError("Should not happen!");
        }
    }

    public Snapshot(int totalAttributesNumber) {
        this.data = new AtomicReferenceArray<>(totalAttributesNumber);
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
     *
     *
     * @return Iterator that iterates over copies of underlying arrays
     */
    @Override
    public final Iterator<SingleRecord> iterator() {
        final int size = data.length();
        final SingleRecord[] data = new SingleRecord[size];
        try {
            System.arraycopy(
                    array.get(this.data),0,data,0,size);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Can not happen!");
        }

            return new Iterator<SingleRecord>() {
                private int pos = 0;

                @Override
                public boolean hasNext() {
                    return pos < size;
                }

                @Override
                public SingleRecord next() {
                    return data[pos++];
                }
            };

    }

    @Override
    public void forEach(Consumer<? super SingleRecord> action) {
        for(SingleRecord record : this){
            action.accept(record);
        }
    }

    @Override
    public Spliterator<SingleRecord> spliterator() {
        throw new UnsupportedOperationException("This method is not supported in " + this.getClass());
    }
}
