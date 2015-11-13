package wpn.hdri.ss.data2;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.LongBuffer;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

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
    public static final int PRECISION = 100;

    public static final int SEARCH_THRESHOLD_L4 = 1000000;
    public static final int SEARCH_THRESHOLD_L3 =  100000;
    public static final int SEARCH_THRESHOLD_L2 =   10000;
    public static final int SEARCH_THRESHOLD_L1 =    1000;

    public static final List<SingleRecord<?>> EMPTY_RESULT = Collections.emptyList();

    private final int totalNumberOfAttributes;
    private final CopyOnWriteArrayList<SingleRecord<?>> data = new CopyOnWriteArrayList<>();

    public AllRecords(int totalNumberOfAttributes) {
        this.totalNumberOfAttributes = totalNumberOfAttributes;
    }

    public void add(SingleRecord record){
        data.add(record);
    }

    /**
     *  Returns a pair of closest records for each attribute. Any of the pairs can contain same element
     *
     * @param t
     * @return a pair of closest records for each attribute
     */
    public Iterable<SingleRecord<?>> getSnapshot(long t){
        if(data.size() == 0) return EMPTY_RESULT;

        SingleRecord<?>[] array = data.toArray(new SingleRecord<?>[data.size()]);

        List<SingleRecord<?>> result = new ArrayList<>();

        int foundNdx = findLeftNdx(t, array);
        for(int id = 0;id < totalNumberOfAttributes; ++id) {
            int leftNdx = foundNdx;
            //go left while
            do{
                leftNdx--;
            }while(leftNdx >= 0 && array[leftNdx].id != id);
            if(leftNdx == -1) continue;//skip non-recorded attribute

            int rightNdx = foundNdx;
            //go right while
            while(rightNdx < array.length && array[rightNdx].id != id){
                rightNdx++;
            }
            if(rightNdx == array.length) rightNdx = leftNdx;

            result.add(array[leftNdx]);
            result.add(array[rightNdx]);
        }

        return result;
    }

    /**
     *
     * @param t0 a read timestamp
     * @return all records that were added after specified timestamp, aka getRange(t0, Long.MAX_VALUE)
     */
    public Iterable<SingleRecord<?>> getRange(long t0){
        if(data.size() == 0) return EMPTY_RESULT;

        SingleRecord<?>[] array = data.toArray(new SingleRecord<?>[data.size()]);

        if(t0 <= array[0].r_t){
            return Arrays.asList(array);
        } else if(t0 >= array[array.length - 1].r_t) {
            return EMPTY_RESULT;
        } else {
            int leftNdx = findLeftNdx(t0, array);



            return Arrays.asList(array).subList(leftNdx, array.length);
        }
    }

    /**
     * Finds ndx so that array[ndx-1].r_t <= t && array[ndx].r_t > t
     *
     * @param t
     * @param array
     * @return
     */
    private int findLeftNdx(long t, SingleRecord<?>[] array) {
        if(t <= array[0].r_t) return 0;
        if(t >= array[array.length - 1].r_t) return array.length - 1;
        int size = array.length;
        int startNdx = 0;

        if(size > SEARCH_THRESHOLD_L4 && array[SEARCH_THRESHOLD_L4].r_t < t) {
            startNdx = SEARCH_THRESHOLD_L3;
        } else if(size > SEARCH_THRESHOLD_L3 && array[SEARCH_THRESHOLD_L3].r_t < t) {
            startNdx = SEARCH_THRESHOLD_L3;
        } else if(size > SEARCH_THRESHOLD_L2 && array[SEARCH_THRESHOLD_L2].r_t < t){
            startNdx = SEARCH_THRESHOLD_L2;
        } else if (size > SEARCH_THRESHOLD_L1 && array[SEARCH_THRESHOLD_L1].r_t < t){
            startNdx = SEARCH_THRESHOLD_L1;
        }

        int result = Arrays.binarySearch(array, startNdx, array.length, new SingleRecord<>(null, t, 0L, null), new TimeComparator());

        if(result < 0) result = Math.abs(result + 1);
        //if t is left to result move left while t LE result
        if(t < array[result].r_t)
            while (result - 1 > 0  && t <= array[result - 1].r_t){
                result--;
            }
            //if t is right to result move left while t LE result
        else if(t > array[result].r_t)
            do{//by definition we should move at least once in this case
                result++;
            } while (result + 1 < array.length && t >= array[result + 1].r_t);

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

    /**
     * Finds ndx so that array[ndx].r_t <= t && t < array[ndx+1].r_t
     *
     * @param t
     * @param array
     * @param leftNdx
     * @return
     */
    private int findRightNdx(long t, SingleRecord<?>[] array, int leftNdx) {
        if(t >= array[array.length - 1].r_t) return array.length - 1;

        int size = array.length;
        int endNdx = array.length;
        if(size > SEARCH_THRESHOLD_L4 && t < array[size - SEARCH_THRESHOLD_L4].r_t) {
            endNdx = size - SEARCH_THRESHOLD_L4;
        } else if(size > SEARCH_THRESHOLD_L3 && t < array[size - SEARCH_THRESHOLD_L3].r_t) {
            endNdx = size - SEARCH_THRESHOLD_L3;
        } else if(size > SEARCH_THRESHOLD_L2 && t < array[size - SEARCH_THRESHOLD_L2].r_t){
            endNdx = size - SEARCH_THRESHOLD_L2;
        } else if (size > SEARCH_THRESHOLD_L1 && t < array[size - SEARCH_THRESHOLD_L1].r_t){
            endNdx = size - SEARCH_THRESHOLD_L1;
        }


        int result = Arrays.binarySearch(array, leftNdx, endNdx, new SingleRecord<>(null, t, 0L, null), new TimeComparator());

        if(result < 0) result = Math.abs(result + 1);
        //if t is right to result move left while t GE result
        if(t > array[result].r_t)
            while (result + 1 < array.length  && t >= array[result + 1].r_t){
                result++;
            }
            //if t is left to result move right while t LE result
        else if(t < array[result].r_t)
            do{
                result--;
            } while (result - 1 > 0 && t <= array[result - 1].r_t);

        return result;
    }

    private static class TimeComparator implements Comparator<SingleRecord<?>>{
        @Override
        public int compare(SingleRecord<?> o1, SingleRecord<?> o2) {
            long diff = o1.r_t - o2.r_t;
            return Math.abs(diff) < PRECISION ? 0 : diff > PRECISION ? 1 : -1;
        }
    }
}
