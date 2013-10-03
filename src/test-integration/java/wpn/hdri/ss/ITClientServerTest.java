package wpn.hdri.ss;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import wpn.hdri.ss.tango.StatusServerStub;
import wpn.hdri.tango.proxy.TangoProxy;

import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 20.06.13
 */
public class ITClientServerTest {
    private final static ExecutorService STATUS_SERVER_EXECUTOR = Executors.newSingleThreadExecutor();
    @BeforeClass
    public static void before() throws Exception{
        final CountDownLatch start = new CountDownLatch(1);
        STATUS_SERVER_EXECUTOR.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Launcher.main(new String[]{"development","-v","-c=src/main/conf/StatusServer.configuration.xml"});
                    start.countDown();
                } catch (Exception e) {
                    assertTrue(false);
                    throw new RuntimeException(e);
                }
            }
        });
        start.await();
    }

    @AfterClass
    public static void after(){
        STATUS_SERVER_EXECUTOR.shutdownNow();
    }

    @Test
    public void test() throws Exception {
        StatusServerStub instance = TangoProxy.proxy("tango://localhost:10000/development/1.0.1-SNAPSHOT/0", StatusServerStub.class);

        System.out.println(instance.getStatus());
        //TODO class cast exception
//        System.out.println(instance.getState());
        System.out.println(instance.getStatus());

//        instance.startCollectData();

        instance.setUseAliases(true);

        System.out.println(instance.getStatus());

        for (int i = 0; i < 100000; ++i) {
            instance.getLatestSnapshot();
        }

        long start = System.nanoTime();
        for (int i = 0; i < 10000; ++i) {
            instance.getLatestSnapshot();
        }
        long end = System.nanoTime();

        long delta = end - start;
        long average = delta / 10000;

        System.out.println("Delta time in getLatestValues (nano) = " + delta);
        System.out.println("Delta time in getLatestValues (millis) = " + TimeUnit.NANOSECONDS.toMillis(delta));

        System.out.println("Average time in getLatestValues (nano) = " + average);
        System.out.println("Average time in getLatestValues (millis) = " + TimeUnit.NANOSECONDS.toMillis(average));
        System.out.println("Average time in getLatestValues (seconds) = " + TimeUnit.NANOSECONDS.toSeconds(average));

//        instance.stopCollectData();

//        System.out.println(instance.getCrtActivity());

//        assertTrue(average < 100000);
    }

    @Test
    public void testMultithreading() throws Exception {
        ExecutorService exec = Executors.newFixedThreadPool(2);

        final CyclicBarrier done = new CyclicBarrier(3);

        exec.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                ITClientServerTest.this.test();
                done.await();
                return null;
            }
        });

        exec.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                ITClientServerTest.this.test();
                done.await();
                return null;
            }
        });

        done.await();

        exec.shutdown();
    }
}
