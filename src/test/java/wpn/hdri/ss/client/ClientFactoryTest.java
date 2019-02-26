package wpn.hdri.ss.client;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/26/19
 */
public class ClientFactoryTest {

    @Test
    public void createTineClient() throws Exception {
        ClientFactory instance = new ClientFactory();

        TineClient result = (TineClient) instance.createClient("tine:/PETRA/Idc/Buffer-0/I.SCH");

        assertEquals("PETRA", result.context);
        assertEquals("Idc", result.serverName);
        assertEquals("Buffer-0", result.deviceName);
    }

    @Test
    public void createTangoClient() throws Exception {
        ClientFactory instance = new ClientFactory();

        TangoClient result = (TangoClient) instance.createClient("tango://tango_host:10000/sys/tg_test/1");

        assertEquals("tango://tango_host:10000/sys/tg_test/1", result.getDeviceName());
    }
}