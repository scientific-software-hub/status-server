package wpn.hdri.ss.engine;

import org.junit.Test;
import wpn.hdri.ss.client.ClientFactory;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.configuration.StatusServerProperties;

import java.util.Collections;

import static junit.framework.Assert.assertTrue;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.06.13
 */
public class EngineInitializerTest {
    private final String xmlConfigPath = "target/test-classes/conf/StatusServer.test.xml";

    /**
     * This test checks how clients is being initialized if case of an error - no such device exists
     *
     * @throws Exception
     */
    @Test
    public void testBadClient() throws Exception {
        EngineInitializer instance = new EngineInitializer(StatusServerConfiguration.fromXml(xmlConfigPath), new StatusServerProperties(Collections.emptyList()));

        ClientsManager result = instance.initializeClients();

        assertTrue(result.getClient("Test.Device") instanceof ClientFactory.BadClient);
    }

    @Test
    public void testBadAttribute() throws Exception {
        //TODO integrate google guice in EngineInitializer
    }
}
