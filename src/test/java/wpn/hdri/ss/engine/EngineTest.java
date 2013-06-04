/*
 * The main contributor to this project is Institute of Materials Research,
 * Helmholtz-Zentrum Geesthacht,
 * Germany.
 *
 * This project is a contribution of the Helmholtz Association Centres and
 * Technische Universitaet Muenchen to the ESS Design Update Phase.
 *
 * The project's funding reference is FKZ05E11CG1.
 *
 * Copyright (c) 2012. Institute of Materials Research,
 * Helmholtz-Zentrum Geesthacht,
 * Germany.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package wpn.hdri.ss.engine;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.client.ClientException;
import wpn.hdri.ss.client.ClientFactory;
import wpn.hdri.ss.configuration.ConfigurationBuilder;
import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.storage.Storage;

import java.util.AbstractMap;

import static org.mockito.Mockito.*;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 04.05.12
 */
public class EngineTest {
    private final String xmlConfig = "target/test-classes/conf/StatusServer.test.xml";
    private final ConfigurationBuilder conf = new ConfigurationBuilder();
    private Storage mockStorage;
    private Logger mockLogger;

    private AttributesManager attributesManager;

    @Before
    public void before() {
        mockStorage = mock(Storage.class);
        mockLogger = spy(new Logger(EngineTest.class.getSimpleName()) {
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

        attributesManager = new AttributesManager();
    }

    @Test
    public void test() throws Exception {
        ClientsManager clientsManager = new ClientsManager(new ClientFactory() {
            @Override
            public Client createClient(String deviceName) {
                Client client = mock(Client.class);
                try {
                    //TODO replace anyString()
                    doReturn(true).when(client).checkAttribute(anyString());
                    doReturn(new AbstractMap.SimpleEntry<String, Timestamp>("some value", new Timestamp(System.currentTimeMillis())))
                            .when(client).readAttribute(anyString());
                    doReturn(String.class).when(client).getAttributeClass(anyString());
                } catch (ClientException e) {
                    throw new RuntimeException(e);
                }

                return client;
            }
        });

        Engine engine = new Engine(conf.fromXml(xmlConfig), null, clientsManager, attributesManager, mockLogger, 2);

        engine.initialize();

        engine.start(1);
        Thread.sleep(10);
        engine.stop();

        //TODO rewrite this test as we can not rely on logger any more (it was disabled due to performance issues)
        //verify(mockLogger, atLeastOnce()).info("Read attribute Test.Device/Test.Attribute: some value");
        engine.shutdown();
    }

    @Test
    public void testBadClient() throws Exception {
        ClientsManager clientsManager = new ClientsManager(new ClientFactory());

        Engine engine = new Engine(conf.fromXml(xmlConfig), null, clientsManager, attributesManager, mockLogger, 2);

        engine.initialize();

        //BadClient does not have bad attributes - all attributes are good, but they all have String value "Bad value"
        //therefore initialization will succeed.
        //see BadClient impl.
        verify(mockLogger, atLeastOnce()).info("Initialization succeed.");
    }

    //TODO what does this test actually do?!
    @Test
    public void testBadAttribute() throws Exception {
        ClientsManager clientsManager = new ClientsManager(new ClientFactory());

        Engine engine = new Engine(conf.fromXml(xmlConfig), null, clientsManager, attributesManager, mockLogger, 2);

        engine.initialize();

        engine.start(1);
        Thread.sleep(10);
        engine.stop();

        engine.shutdown();

        verify(mockLogger, atLeastOnce()).info("Scheduling read task for Test.Device/Test.Attribute");
        verify(mockLogger, atLeastOnce()).info("Subscribing for changes from Test.Device/Test.Attribute.1");
    }

    @Test
    public void testTolerateExceptionsDuringWork() throws Exception {
        ClientsManager clientsManager = new ClientsManager(new ClientFactory() {
            @Override
            public Client createClient(String deviceName) {
                Client client = mock(Client.class);

                try {
                    //TODO replace anyString
                    doReturn(true).when(client).checkAttribute(anyString());
                    doThrow(new RuntimeException("Holy mother of God!")).when(client).readAttribute(anyString());
                    doReturn(String.class).when(client).getAttributeClass(anyString());
                } catch (ClientException e) {
                    throw new RuntimeException(e);
                }


                return client;
            }
        });

        Engine engine = new Engine(conf.fromXml(xmlConfig), null, clientsManager, attributesManager, mockLogger, 2);

        engine.initialize();

        engine.start(1);
        Thread.sleep(7000);
        engine.stop();

        verify(mockLogger, atLeastOnce()).error(eq("An attempt to read attribute Test.Device/Test.Attribute has failed. Tries left: 2"), any(Throwable.class));
        verify(mockLogger, atLeast(2)).error(eq("All attempts to read attribute Test.Device/Test.Attribute failed. Writing null."), any(Throwable.class));

        engine.shutdown();
    }
}
