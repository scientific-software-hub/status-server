package StatusServer;

import org.apache.log4j.Logger;
import org.junit.Test;
import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.client.ClientException;
import wpn.hdri.ss.client.ClientFactory;
import wpn.hdri.ss.configuration.ConfigurationBuilder;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.engine.AttributeFilters;
import wpn.hdri.ss.engine.AttributesManager;
import wpn.hdri.ss.engine.ClientsManager;
import wpn.hdri.ss.engine.Engine;
import wpn.hdri.ss.storage.StorageFactory;

import java.util.AbstractMap;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 11.06.13
 */
public class ITStatusServerStressTest {
    private static final Logger LOG = Logger.getLogger(ITStatusServerStressTest.class);
    private static final String CONFIGURATION = "target/test-classes/conf/StatusServer.integration.test.xml";

    @Test
    public void testGetLatestSnapshot() throws Exception{
        StatusServerConfiguration configuration = new ConfigurationBuilder().addDevice("fake").addAttributeToDevice("fake","double","poll","last",1,0).build();

        ClientsManager clientsManager = new ClientsManager(new ClientFactory() {
            @Override
            public Client createClient(String deviceName) {
                Client client = mock(Client.class);
                try {
                    //TODO replace anyString
                    doReturn(true).when(client).checkAttribute(anyString());
                    doReturn(new AbstractMap.SimpleEntry<Double, Timestamp>(Math.random(), new Timestamp(System.currentTimeMillis())))
                            .when(client).readAttribute(anyString());
                    doReturn(double.class).when(client).getAttributeClass(anyString());
                } catch (ClientException e) {
                    throw new RuntimeException(e);
                }

                return client;
            }
        });

        Engine engine = new Engine(configuration,new StorageFactory(), clientsManager, new AttributesManager(), LOG, 2);

        engine.initialize();

        engine.start();

        //collect aprox 1M values
        Thread.sleep(1000000);

        long start = System.nanoTime();
        for(int i = 0; i<1000; i++){
            engine.getLatestValues(AttributeFilters.none());
        }
        long stop = System.nanoTime();

        long delta = stop - start;
        System.out.println("Average time in getLatestValues (nano) = " + (delta / 1000));
        System.out.println("Average time in getLatestValues (millis) = " + TimeUnit.MILLISECONDS.convert(delta / 1000,TimeUnit.NANOSECONDS));
        System.out.println("Average time in getLatestValues (seconds) = " + TimeUnit.SECONDS.convert(delta / 1000,TimeUnit.NANOSECONDS));
    }

    @Test
    public void testGetSnapshot(){

    }

    @Test
    public void testGetDataUpdates(){

    }

    @Test
    public void testGetData(){

    }

}
