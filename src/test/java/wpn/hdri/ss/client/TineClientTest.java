package wpn.hdri.ss.client;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 3/1/19
 */
public class TineClientTest {

    @Test
    public void getDeviceName() {
        TineClient instance = new TineClient(URI.create("tine:/PETRA/Globals/#keyword"));

        String result = instance.getTineName();

        assertEquals("/PETRA/Globals/#keyword", result);
    }
}