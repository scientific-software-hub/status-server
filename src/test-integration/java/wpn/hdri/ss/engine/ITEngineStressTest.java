package wpn.hdri.ss.engine;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import wpn.hdri.ss.EngineTestBootstrap;
import wpn.hdri.ss.data.Timestamp;

import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.spy;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 14.06.13
 */
public class ITEngineStressTest {
    public static final int _1M = 999999 + 2;//force persistent file creation
    public static final int _10K = 10000;
    public static final int _100K = 100000;

    private final Logger mockLogger = spy(new Logger(ITEngineStressTest.class.getSimpleName()) {
        @Override
        public void info(Object message) {
            System.out.println(message);
        }

        @Override
        public void error(Object message, Throwable t) {
            System.err.println(message);
        }

        @Override
        public void error(Object message) {
            System.err.println(message);
        }
    });

    private Engine engine;

    @Before
    public void before() {
        EngineTestBootstrap bootstrap = new EngineTestBootstrap();
        bootstrap.bootstrap();
        engine = bootstrap.getEngine();
    }

    @After
    public void after() {
        engine.clear();
        engine = null;
    }

    @Test
    public void testGetLatestSnapshot() throws Exception {
        for (int i = 0; i < _100K; i++) {
            engine.getLatestValues(AttributesManager.DEFAULT_ATTR_GROUP);
        }

        long start = System.nanoTime();
        for (int i = 0; i < _10K; i++) {
            engine.getLatestValues(AttributesManager.DEFAULT_ATTR_GROUP);
        }
        long stop = System.nanoTime();

        long delta = stop - start;
        long average = delta / _10K;
        System.out.println("Average time in getLatestValues (nano) = " + average);
        System.out.println("Average time in getLatestValues (millis) = " + TimeUnit.MILLISECONDS.convert(average, TimeUnit.NANOSECONDS));
        System.out.println("Average time in getLatestValues (seconds) = " + TimeUnit.SECONDS.convert(average, TimeUnit.NANOSECONDS));

        assertTrue(average < _100K);
    }

    @Test
    public void testGetSnapshot() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis() - 1000);
        for (int i = 0; i < _100K; i++) {
            engine.getValues(timestamp, AttributesManager.DEFAULT_ATTR_GROUP);
        }

        long start = System.nanoTime();
        for (int i = 0; i < _10K; i++) {
            engine.getValues(timestamp, AttributesManager.DEFAULT_ATTR_GROUP);
        }
        long stop = System.nanoTime();

        long delta = stop - start;
        long average = delta / _10K;
        System.out.println("Average time in getLatestValues (nano) = " + average);
        System.out.println("Average time in getLatestValues (millis) = " + TimeUnit.MILLISECONDS.convert(average, TimeUnit.NANOSECONDS));
        System.out.println("Average time in getLatestValues (seconds) = " + TimeUnit.SECONDS.convert(average, TimeUnit.NANOSECONDS));

        assertTrue(average < _100K);
    }

    @Test
    public void testGetDataUpdates() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis() - 1000);
        for (int i = 0; i < _100K; i++) {
            engine.getAllAttributeValues(timestamp, AttributesManager.DEFAULT_ATTR_GROUP);
        }


        long start = System.nanoTime();
        for (int i = 0; i < _10K; i++) {
            engine.getAllAttributeValues(timestamp, AttributesManager.DEFAULT_ATTR_GROUP);
        }
        long stop = System.nanoTime();

        long delta = stop - start;
        long average = delta / _10K;
        System.out.println("Average time in getLatestValues (nano) = " + average);
        System.out.println("Average time in getLatestValues (millis) = " + TimeUnit.MILLISECONDS.convert(average, TimeUnit.NANOSECONDS));
        System.out.println("Average time in getLatestValues (seconds) = " + TimeUnit.SECONDS.convert(average, TimeUnit.NANOSECONDS));

        assertTrue(average < _100K);
    }

    @Test
    public void testGetData() {
        for (int i = 0; i < _100K; i++) {
            engine.getAllAttributeValues(Timestamp.DEEP_PAST, AttributesManager.DEFAULT_ATTR_GROUP);
        }

        long start = System.nanoTime();
        for (int i = 0; i < _10K; i++) {
            engine.getAllAttributeValues(Timestamp.DEEP_PAST, AttributesManager.DEFAULT_ATTR_GROUP);
        }
        long stop = System.nanoTime();

        long delta = stop - start;
        long average = delta / _10K;
        System.out.println("Average time in getLatestValues (nano) = " + average);
        System.out.println("Average time in getLatestValues (millis) = " + TimeUnit.MILLISECONDS.convert(average, TimeUnit.NANOSECONDS));
        System.out.println("Average time in getLatestValues (seconds) = " + TimeUnit.SECONDS.convert(average, TimeUnit.NANOSECONDS));

        assertTrue(average < _100K);
    }

    public static void main(String... args) throws Exception {
        ITEngineStressTest testSuite = new ITEngineStressTest();
        testSuite.before();

        testSuite.testGetData();

        testSuite.after();
    }
}
