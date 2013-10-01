package wpn.hdri.ss;

import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.client.ClientException;
import wpn.hdri.ss.client.ClientFactory;
import wpn.hdri.ss.configuration.ConfigurationBuilder;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.data.Interpolation;
import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.data.Value;
import wpn.hdri.ss.data.attribute.Attribute;
import wpn.hdri.ss.data.attribute.AttributeFactory;
import wpn.hdri.ss.data.attribute.NumericAttribute;
import wpn.hdri.ss.engine.AttributesManager;
import wpn.hdri.ss.engine.ClientsManager;
import wpn.hdri.ss.engine.Engine;

import java.math.BigDecimal;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 19.06.13
 */
public class EngineTestBootstrap {
    public static final int _1M = 1000000;

    private Engine engine;

    public void bootstrap() {
        StatusServerConfiguration configuration = new ConfigurationBuilder().addDevice("fake").addAttributeToDevice("fake", "double", "poll", "last", 1, 0).build();

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
        final NumericAttribute<Double> doubleAttribute = new NumericAttribute<Double>("fake", "double", Interpolation.LAST, 0.);
        for (int i = 0; i < _1M; ++i) {
            long currentTimestamp = timestamp + i * 1000;
            Timestamp writeTimestamp = new Timestamp(currentTimestamp);
            doubleAttribute.addValue(writeTimestamp, Value.getInstance(Math.random()), writeTimestamp);
        }


        AttributesManager attributesManager = new AttributesManager(new AttributeFactory() {
            @Override
            public Attribute<?> createAttribute(String attrName, String attrAlias, String devName, Interpolation interpolation, BigDecimal precision, Class<?> type, boolean isArray) {
                return doubleAttribute;
            }
        });


        engine = new Engine(clientsManager, attributesManager, 2);
    }

    public Engine getEngine() {
        return engine;
    }
}
