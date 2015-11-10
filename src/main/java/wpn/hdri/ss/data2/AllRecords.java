package wpn.hdri.ss.data2;

import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

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
    public static final List EMPTY_RESULT = Collections.EMPTY_LIST;
    private final CopyOnWriteArrayList<SingleRecord<?>> data = new CopyOnWriteArrayList<>();

    public void add(SingleRecord record){
        data.add(record);
    }

    /**
     *
     * @param rTimestamp a read timestamp
     * @return all records that were added after specified timestamp
     */
    public Iterable<SingleRecord<?>> get(long rTimestamp){
        if(data.size() == 0) return EMPTY_RESULT;

        SingleRecord<?>[] array = data.toArray(new SingleRecord<?>[data.size()]);

        if(rTimestamp <= array[0].r_t){
            return Arrays.asList(array);
        } else if(rTimestamp >= array[array.length - 1].r_t) {
            return EMPTY_RESULT;
        } else {
            int leftNdx = findLeftNdx(rTimestamp, array);

            return Arrays.asList(array).subList(leftNdx, array.length);
        }
    }

    private int findLeftNdx(long rTimestamp, SingleRecord<?>[] array) {
        if(rTimestamp <= array[0].r_t) return 0;
        int result = Arrays.binarySearch(array, 0, array.length, new SingleRecord<>(0, rTimestamp, 0L, null), new Comparator<SingleRecord>() {
            @Override
            public int compare(SingleRecord o1, SingleRecord key) {
                return key.r_t <= o1.r_t ? 0 : -1;
            }
        });
        //move left while condition is met
         while (rTimestamp < array[result - 1].r_t){
             result--;
         }

        return result;
    }

    /**
     *
     */
    public Iterable<SingleRecord<?>> getRange(long t0, long t1){
        if(t1 <= t0) throw  new IllegalArgumentException(String.format("Invalid timestamps range: %d, %d", t0, t1));

        if(data.size() == 0) return EMPTY_RESULT;


        SingleRecord<?>[] array = data.toArray(new SingleRecord<?>[data.size()]);

        if (checkIsNotInRange(t0, t1, array)) return EMPTY_RESULT;

        int leftNdx = findLeftNdx(t0, array);

        int rightNdx = findRightNdx(t1, array, leftNdx);

        return Arrays.asList(array).subList(leftNdx, rightNdx + 1);
    }

    private boolean checkIsNotInRange(long t0, long t1, SingleRecord[] array) {
        return t1 <= array[0].r_t ||
                array[array.length - 1].r_t <= t0;
    }

    private int findRightNdx(long t1, SingleRecord<?>[] array, int leftNdx) {
        if(t1 >= array[array.length - 1].r_t) return array.length - 1;
        int result = Arrays.binarySearch(array, leftNdx, array.length, new SingleRecord<>(0, t1, 0L, null), new Comparator<SingleRecord>() {
            @Override
            public int compare(SingleRecord o1, SingleRecord key) {
                return o1.r_t <= key.r_t ? 0 : 1;
            }
        });

        //move right while condition is met
        while (t1 > array[result + 1].r_t){
            result++;
        }

        return result;
    }
}
