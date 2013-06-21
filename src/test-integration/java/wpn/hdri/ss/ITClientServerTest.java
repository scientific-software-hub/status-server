package wpn.hdri.ss;

import org.junit.Before;
import org.junit.Test;
import wpn.hdri.ss.tango.JStatusServerStub;
import wpn.hdri.tango.proxy.TangoProxy;

import java.util.Arrays;
import java.util.concurrent.*;

import static junit.framework.Assert.assertTrue;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 20.06.13
 */
public class ITClientServerTest {
    @Before
    public void before(){
        //TODO start up server
    }

    @Test
    public void test() throws Exception{
        JStatusServerStub instance = TangoProxy.proxy("tango://hzgharwi3:10000/development/ss-1.0.0/0",JStatusServerStub.class);

        System.out.println(instance.getCrtActivity());
        //TODO class cast exception
//        System.out.println(instance.getState());
        System.out.println(instance.getStatus());

//        instance.startCollectData();

        instance.setUseAliases(true);
        System.out.println(instance.isUseAliases());

        System.out.println(instance.getCrtActivity());

        for(int i = 0; i<100000;++i){
            instance.getLatestSnapshot();
        }

        long start = System.nanoTime();
        for(int i = 0; i<10000;++i){
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
    public void testMultithreading() throws Exception{
        ExecutorService exec  = Executors.newFixedThreadPool(2);

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
