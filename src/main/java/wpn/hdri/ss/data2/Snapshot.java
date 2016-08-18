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

    protected final long arrayBaseOffset;
    protected final long arrayIndexScale;

    public Snapshot(int totalAttributesNumber) {
        this.data = new AtomicReferenceArray<>(totalAttributesNumber);
        arrayBaseOffset = UnsafeSupport.UNSAFE.arrayBaseOffset(getArray().getClass());
        arrayIndexScale = UnsafeSupport.UNSAFE.arrayIndexScale(getArray().getClass());
    }

    protected Snapshot(@Nullable Object[] array){
        if (array == null ) this.data = null;
        else this.data = new AtomicReferenceArray(array);
        arrayBaseOffset = UnsafeSupport.UNSAFE.arrayBaseOffset(array.getClass());
        arrayIndexScale = UnsafeSupport.UNSAFE.arrayIndexScale(array.getClass());
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
        final long size = data.length();
        final Object array = getArray();

        return new Iterator<SingleRecord<?>>() {
            private int pos = 0;

            @Override
            public boolean hasNext() {
                return pos < size;
            }

            @Override
            public SingleRecord next() {
                return get(array, pos++);
            }
        };
    }

    public SingleRecord<?> get(int ndx){
        return data.get(ndx);
    }

    /**
     *
     * @param array must be the reference obtained via {@link this#getArray}
     * @param ndx position
     * @return SingleRecord stored in the array at position = ndx
     */
    protected SingleRecord<?> get(Object array, int ndx){
        return SingleRecord.class.cast(UnsafeSupport.UNSAFE.getObject(array, arrayBaseOffset + ndx * arrayIndexScale));
    }
}
