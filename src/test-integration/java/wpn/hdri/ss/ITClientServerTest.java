package wpn.hdri.ss;

import org.junit.Before;
import org.junit.Test;
import wpn.hdri.ss.tango.JStatusServerStub;
import wpn.hdri.tango.proxy.TangoProxy;

import java.util.Arrays;

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

        instance.startCollectData();

        instance.setUseAliases(true);
        System.out.println(instance.isUseAliases());

        System.out.println(instance.getCrtActivity());
        System.out.println(Arrays.toString(instance.getLatestSnapshot()));

        instance.stopCollectData();

        System.out.println(instance.getCrtActivity());
    }
}
