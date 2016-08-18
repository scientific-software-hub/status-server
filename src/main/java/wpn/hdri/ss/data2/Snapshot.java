package wpn.hdri.ss.data2;

import hzg.wpn.UnsafeSupport;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
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

    private static final long FLD_ARRAY_OFFSET;
    static {
        try {
            Field fldArray = AtomicReferenceArray.class.getDeclaredField("array");

            FLD_ARRAY_OFFSET = UnsafeSupport.UNSAFE.objectFieldOffset(fldArray);
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Should not happen!");
        }
    }

    public Snapshot(int totalAttributesNumber) {
        this.data = new AtomicReferenceArray<>(totalAttributesNumber);
    }

    protected Snapshot(@Nullable Object[] array){
        if (array == null ) this.data = null;
        else this.data = new AtomicReferenceArray(array);
    }

    protected Object getArray(){
        return UnsafeSupport.UNSAFE.getObject(this.data, FLD_ARRAY_OFFSET);
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
    public final Iterator<SingleRecord<?>> iterator() {
        final int size = data.length();
        final SingleRecord[] data = new SingleRecord[size];
            System.arraycopy(
                    getArray(),0,data,0,size);

            return new Iterator<SingleRecord<?>>() {
                private int pos = 0;

                @Override
                public boolean hasNext() {
                    return pos < size;
                }

                @Override
                public SingleRecord next() {
                    return data[pos++];
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("This method is not supported in " + this.getClass());
                }
            };

    }

    public SingleRecord<?> get(int ndx){
        return data.get(ndx);
    }
}
