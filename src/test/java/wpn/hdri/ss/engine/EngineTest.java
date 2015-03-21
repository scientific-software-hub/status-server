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

import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.client.ClientFactory;
import wpn.hdri.ss.configuration.ConfigurationBuilder;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.configuration.StatusServerProperties;
import wpn.hdri.ss.data.Value;
import wpn.hdri.ss.data.attribute.AttributeName;
import wpn.hdri.ss.data.attribute.AttributeValue;
import wpn.hdri.ss.storage.Storage;

import static junit.framework.Assert.assertSame;
import static org.mockito.Mockito.*;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 04.05.12
 */
public class EngineTest {
    private final String xmlConfigPath = "target/test-classes/conf/StatusServer.test.xml";
    private final ConfigurationBuilder conf = new ConfigurationBuilder();
    private Storage mockStorage;
    private Logger mockLogger;

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
    }

    @Test
    public void testTolerateExceptionsDuringWork() throws Exception {
        final Client client = mock(Client.class);

        doReturn(true).when(client).checkAttribute(anyString());
        doThrow(new RuntimeException("Holy mother of God!")).when(client).readAttribute(anyString());
        doReturn(String.class).when(client).getAttributeClass(anyString());


        ClientsManager clientsManager = new ClientsManager(new ClientFactory() {
            @Override
            public Client createClient(String deviceName) {
                return client;
            }
        }) {
            @Override
            public Client getClient(String name) {
                return client;
            }
        };

        EngineInitializer initializer = new EngineInitializer(StatusServerConfiguration.fromXml(xmlConfigPath), new StatusServerProperties());

        AttributesManager attributesManager = initializer.initializeAttributes(clientsManager);

        Engine engine = new Engine(clientsManager, attributesManager, null, 2);
        engine.submitPollingTasks(initializer.initializePollTasks(clientsManager, attributesManager));

        engine.start(0);
        Thread.sleep(7000);
        engine.stop();

        Multimap<AttributeName, AttributeValue<?>> values = engine.getLatestValues(AttributesManager.DEFAULT_ATTR_GROUP);

        assertSame(Value.NULL, Iterables.getFirst(values.asMap().get(new AttributeName("Test.Device", "Test.Attribute", null)), null).getValue());
//        this produces NPE because event based attribute has never been updated
//        assertEquals("NA", Iterables.getLast(values.values(),null).getValue().get());

        engine.shutdown();
    }
}
