package StatusServer;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.client.ClientException;
import wpn.hdri.ss.client.ClientFactory;
import wpn.hdri.ss.configuration.ConfigurationBuilder;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.data.*;
import wpn.hdri.ss.engine.AttributeFilters;
import wpn.hdri.ss.engine.AttributesManager;
import wpn.hdri.ss.engine.ClientsManager;
import wpn.hdri.ss.engine.Engine;
import wpn.hdri.ss.storage.StorageFactory;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 14.06.13
 */
public class ITEngineStressTest {
    private static final String CONFIGURATION = "target/test-classes/conf/StatusServer.integration.test.xml";

    public static final int _1M = 999999 +2;//force persistent file creation
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
    public void before(){
        StatusServerConfiguration configuration = new ConfigurationBuilder().addDevice("fake").addAttributeToDevice("fake","double","poll","last",1,0).build();

        ClientsManager clientsManager = new ClientsManager(new ClientFactory() {
            @Override
            public Client createClient(String deviceName) {
                Client client = mock(Client.class);
                try {
                    doReturn(true).when(client).checkAttribute("double");
                    doReturn(double.class).when(client).getAttributeClass("double");
                } catch (ClientException e) {
                    throw new RuntimeException(e);
                }

                return client;
            }
        });

        long timestamp = System.currentTimeMillis();
        final NumericAttribute<Double> doubleAttribute = new NumericAttribute<Double>("fake","double", Interpolation.LAST,0.);
        for(int i = 0; i< _1M; ++i){
            long currentTimestamp = timestamp + i * 1000;
            doubleAttribute.addValue(currentTimestamp, Value.getInstance(Math.random()), currentTimestamp);
        }


        AttributesManager attributesManager = new AttributesManager(new AttributeFactory(){
            @Override
            public Attribute<?> createAttribute(String attrName, String attrAlias, String devName, Interpolation interpolation, BigDecimal precision, Class<?> type, boolean isArray) {
                return doubleAttribute;
            }
        });


        engine = new Engine(configuration,new StorageFactory(), clientsManager, attributesManager, mockLogger, 2);
    }

    @Test
    public void testGetLatestSnapshot() throws Exception{
        engine.initialize();

        long start = System.nanoTime();
        for(int i = 0; i<1000; i++){
            engine.getLatestValues(AttributeFilters.none());
        }
        long stop = System.nanoTime();

        long delta = stop - start;
        long average = delta / 1000;
        System.out.println("Average time in getLatestValues (nano) = " + average);
        System.out.println("Average time in getLatestValues (millis) = " + TimeUnit.MILLISECONDS.convert(average,TimeUnit.NANOSECONDS));
        System.out.println("Average time in getLatestValues (seconds) = " + TimeUnit.SECONDS.convert(average, TimeUnit.NANOSECONDS));

        assertTrue(average < _100K);
    }

    @Test
    public void testGetSnapshot(){
        engine.initialize();

        Timestamp timestamp = new Timestamp(System.currentTimeMillis() - 1000);
        long start = System.nanoTime();
        for(int i = 0; i<1000; i++){
            engine.getValues(timestamp, AttributeFilters.none());
        }
        long stop = System.nanoTime();

        long delta = stop - start;
        long average = delta / 1000;
        System.out.println("Average time in getLatestValues (nano) = " + average);
        System.out.println("Average time in getLatestValues (millis) = " + TimeUnit.MILLISECONDS.convert(average,TimeUnit.NANOSECONDS));
        System.out.println("Average time in getLatestValues (seconds) = " + TimeUnit.SECONDS.convert(average, TimeUnit.NANOSECONDS));

        assertTrue(average < _100K);
    }

    @Test
    public void testGetDataUpdates(){
        engine.initialize();

        Timestamp timestamp = new Timestamp(System.currentTimeMillis() - 1000);
        long start = System.nanoTime();
        for(int i = 0; i<1000; i++){
            engine.getAllAttributeValues(timestamp, AttributeFilters.none());
        }
        long stop = System.nanoTime();

        long delta = stop - start;
        long average = delta / 1000;
        System.out.println("Average time in getLatestValues (nano) = " + average);
        System.out.println("Average time in getLatestValues (millis) = " + TimeUnit.MILLISECONDS.convert(average,TimeUnit.NANOSECONDS));
        System.out.println("Average time in getLatestValues (seconds) = " + TimeUnit.SECONDS.convert(average, TimeUnit.NANOSECONDS));

        assertTrue(average < _100K);
    }

    @Test
    public void testGetData(){
        engine.initialize();

        long start = System.nanoTime();
        for(int i = 0; i<1000; i++){
            engine.getAllAttributeValues(Timestamp.DEEP_PAST, AttributeFilters.none());
        }
        long stop = System.nanoTime();

        long delta = stop - start;
        long average = delta / 1000;
        System.out.println("Average time in getLatestValues (nano) = " + average);
        System.out.println("Average time in getLatestValues (millis) = " + TimeUnit.MILLISECONDS.convert(average,TimeUnit.NANOSECONDS));
        System.out.println("Average time in getLatestValues (seconds) = " + TimeUnit.SECONDS.convert(average, TimeUnit.NANOSECONDS));

        assertTrue(average < _100K);
    }

    public static void main(String... args) throws Exception{
        ITEngineStressTest testSuite = new ITEngineStressTest();

        testSuite.before();

        testSuite.testGetSnapshot();
    }
}
