package wpn.hdri.ss.tango;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import wpn.hdri.ss.EngineTestBootstrap;
import wpn.hdri.ss.engine.Engine;

import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertTrue;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 11.06.13
 */
public class ITStatusServerStressTest {
    public static final int _10K = 10000;
    public static final int _100K = 100000;

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
        StatusServer instance = new StatusServer(engine);

        for (int i = 0; i < _100K; ++i) {
            instance.getLatestSnapshot();
        }

        long start = System.nanoTime();
        for (int i = 0; i < _10K; ++i) {
            instance.getLatestSnapshot();
        }
        long end = System.nanoTime();

        long delta = end - start;
        long average = delta / _10K;

        System.out.println("Average time in getLatestValues (nano) = " + average);
        System.out.println("Average time in getLatestValues (millis) = " + TimeUnit.MILLISECONDS.convert(average, TimeUnit.NANOSECONDS));
        System.out.println("Average time in getLatestValues (seconds) = " + TimeUnit.SECONDS.convert(average, TimeUnit.NANOSECONDS));

        assertTrue(average < _100K);
    }

    @Test
    public void testGetSnapshot() throws Exception {
        long timestamp = System.currentTimeMillis() - 1000;
        StatusServer instance = new StatusServer(engine);

        for (int i = 0; i < _100K; ++i) {
            instance.getSnapshot(timestamp);
        }

        long start = System.nanoTime();
        for (int i = 0; i < _10K; ++i) {
            instance.getSnapshot(timestamp);
        }
        long end = System.nanoTime();

        long delta = end - start;
        long average = delta / _10K;

        System.out.println("Average time in getLatestValues (nano) = " + average);
        System.out.println("Average time in getLatestValues (millis) = " + TimeUnit.MILLISECONDS.convert(average, TimeUnit.NANOSECONDS));
        System.out.println("Average time in getLatestValues (seconds) = " + TimeUnit.SECONDS.convert(average, TimeUnit.NANOSECONDS));

        assertTrue(average < _100K);
    }

    //TODO
    //@Test(expected = OutOfMemoryError.class)
    public void testGetDataEncoded() throws Exception {
        StatusServer instance = new StatusServer(engine);

        for (int i = 0; i < _100K; ++i) {
//            instance.getDataEncoded();
        }

        long start = System.nanoTime();
        for (int i = 0; i < _10K; ++i) {
//            instance.getDataEncoded();
        }
        long end = System.nanoTime();

        long delta = end - start;
        long average = delta / _10K;

        System.out.println("Average time in getLatestValues (nano) = " + average);
        System.out.println("Average time in getLatestValues (millis) = " + TimeUnit.MILLISECONDS.convert(average, TimeUnit.NANOSECONDS));
        System.out.println("Average time in getLatestValues (seconds) = " + TimeUnit.SECONDS.convert(average, TimeUnit.NANOSECONDS));

        assertTrue(average < _100K);
    }

    @Test
    public void testGetData() throws Exception {
        StatusServer instance = new StatusServer(engine);

        for (int i = 0; i < _100K; ++i) {
            instance.getData();
        }

        long start = System.nanoTime();
        for (int i = 0; i < _10K; ++i) {
            instance.getData();
        }
        long end = System.nanoTime();

        long delta = end - start;
        long average = delta / _10K;

        System.out.println("Average time in getLatestValues (nano) = " + average);
        System.out.println("Average time in getLatestValues (millis) = " + TimeUnit.MILLISECONDS.convert(average, TimeUnit.NANOSECONDS));
        System.out.println("Average time in getLatestValues (seconds) = " + TimeUnit.SECONDS.convert(average, TimeUnit.NANOSECONDS));

        assertTrue(average < _100K);
    }
}
