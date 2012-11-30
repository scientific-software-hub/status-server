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

package wpn.hdri.ss.client;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.apache.log4j.Logger;
import wpn.hdri.ss.client.tango.TangoClientFactory;
import wpn.hdri.ss.client.tine.TineClientFactory;
import wpn.hdri.ss.data.Timestamp;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 27.04.12
 */
public class ClientFactory extends AbsClientFactory {
    private final List<AbsClientFactory> factories = Arrays.asList(new TangoClientFactory(), new TineClientFactory());

    /**
     * Creates an instance of either {@link wpn.hdri.ss.client.tango.TangoClient} or {@link wpn.hdri.ss.client.tine.TineClient} or null.
     * <p/>
     * Returning instance depends on which factory will succeed to create a client: Tango will try to create TangoClient
     * and Tine will try to create a TineClient
     * <p/>
     * Null client indicates that all factories failed to create a client, due to any error.
     * <p/>
     * One can investigate these problems by calling {@link this#wasExceptions()} method.
     *
     * @param deviceName a device name to which factories try to connect
     * @return new client (BadClient if neither factory was able to create a client)
     */
    @Override
    public Client createClient(String deviceName) {
        Client result = null;
        int i = 0;
        do {
            result = factories.get(i).createClient(deviceName);
        } while (++i < factories.size() && result == null);
        if (result == null) {
            result = new BadClient(deviceName);
        }
        return result;
    }

    /**
     * Returns {@link Iterable} of Exceptions. These exceptions occurred when factories tried to create a client.
     *
     * @return Iterable of Exceptions
     */
    public Iterable<Exception> wasExceptions() {
        return Iterables.concat(Iterables.transform(factories, new Function<AbsClientFactory, Iterable<Exception>>() {
            @Override
            public Iterable<Exception> apply(AbsClientFactory input) {
                return input.wasExceptions();
            }
        }));
    }

    public static class BadClient extends Client {
        private final Map.Entry<String, Timestamp> readAttributeValue;

        private BadClient(String deviceName) {
            super(deviceName);
            this.readAttributeValue =
                    new AbstractMap.SimpleImmutableEntry<String, Timestamp>("Bad client!!!", Timestamp.now());
        }

        @Override
        public Map.Entry<String, Timestamp> readAttribute(String attrName) {
            return readAttributeValue;
        }

        @Override
        public void writeAttribute(String attrName, Object value) throws ClientException {
            throw new ClientException("This method is not supported in BadClient", null);
        }

        @Override
        public void subscribeEvent(String attrName, EventCallback cbk) throws ClientException {
            cbk.onError(new IllegalStateException("Illegal attempt of subscription to attribute " + attrName + " in BadClient"));
        }

        @Override
        public boolean checkAttribute(String attrName) {
            return true;
        }

        @Override
        public Class<?> getAttributeClass(String attrName) throws ClientException {
            return String.class;
        }

        @Override
        public boolean isArrayAttribute(String attrName) throws ClientException {
            return false;
        }

        @Override
        public void unsubscribeEvent(String attrName) throws ClientException {
            throw new ClientException("Could not unsubscribe from BadClient", new IllegalStateException());
        }

        @Override
        public void printAttributeInfo(String name, Logger logger) {
        }
    }
}
