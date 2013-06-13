package wpn.hdri.ss.data;

import com.google.common.collect.Iterables;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 11.06.13
 */
public class AttributeValuesStorageTest {
    private static final String TEST_ATTR_FULL_NAME = "test/test_double";
    private static final String TEST_ATTR_STORAGE_ROOT = "target/test-classes/storage";
    private static final long TEST_TIMESTAMP = 1000L;

    @Test
    public void testSaveLoad() {
        AttributeValuesStorage<Double> instance = new AttributeValuesStorage<Double>(TEST_ATTR_FULL_NAME, TEST_ATTR_STORAGE_ROOT, 6, 3);

        List<AttributeValue<Double>> expected = new ArrayList<AttributeValue<Double>>();
        for (int i = 0; i < 14; i++) {
            double rnd = Math.random();
            AttributeValue<Double> value = new AttributeValue<Double>(TEST_ATTR_FULL_NAME, null, Value.getInstance(rnd), new Timestamp(TEST_TIMESTAMP + i * 1000), Timestamp.now());
            instance.addValue(value);
            expected.add(value);
        }

        Iterable<AttributeValue<Double>> result = instance.getAllValues();

        assertTrue(Iterables.elementsEqual(expected, result));
    }

    @Test
    public void testConcurrentSaveLoad_SWSR() throws Exception{
        final AttributeValuesStorage<Double> instance = new AttributeValuesStorage<Double>(TEST_ATTR_FULL_NAME, TEST_ATTR_STORAGE_ROOT, 6, 3);

        ExecutorService exec = Executors.newFixedThreadPool(2);

        final List<AttributeValue<Double>> expected = new ArrayList<AttributeValue<Double>>();
        final List<AttributeValue<Double>> result = new ArrayList<AttributeValue<Double>>();

        final CountDownLatch startRead = new CountDownLatch(1);
        final CyclicBarrier allDone = new CyclicBarrier(3);
        exec.submit(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 14; i++) {
                    double rnd = Math.random();
                    AttributeValue<Double> value = new AttributeValue<Double>(TEST_ATTR_FULL_NAME, null, Value.getInstance(rnd), new Timestamp(TEST_TIMESTAMP + i * 1000), Timestamp.now());
                    instance.addValue(value);
                    expected.add(value);
                    if(i == 12){
                        startRead.countDown();
                    }
                }

                try {
                    allDone.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        exec.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    startRead.await();

                    Iterables.addAll(result, instance.getAllValues());

                    try {
                        allDone.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (BrokenBarrierException e) {
                        throw new RuntimeException(e);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

            allDone.await();
            //at least 8 elements should be in the result
            assertTrue(result.size() >= 13);
            //compare first elements
            for(int i = 0, size = result.size();i<size;++i){
                assertEquals(expected.get(i),result.get(i));
            }
    }

    @Test
    public void testClear(){
        AttributeValuesStorage<Double> instance = new AttributeValuesStorage<Double>(TEST_ATTR_FULL_NAME, TEST_ATTR_STORAGE_ROOT, 6, 3);

        List<AttributeValue<Double>> expected = new ArrayList<AttributeValue<Double>>();
        for (int i = 0; i < 14; i++) {
            double rnd = Math.random();
            AttributeValue<Double> value = new AttributeValue<Double>(TEST_ATTR_FULL_NAME, null, Value.getInstance(rnd), new Timestamp(TEST_TIMESTAMP + i * 1000), Timestamp.now());
            instance.addValue(value);
            expected.add(value);
        }

        //simulate Attribute#clear
        instance.persistAndClearInMemoryValues();

        //loads all values from persistent storage
        Iterable<AttributeValue<Double>> result = instance.getAllValues();

        assertTrue(Iterables.elementsEqual(expected, result));
    }

    @Test
    public void testConcurrentClear(){
        //TODO clear while writing
    }
}
