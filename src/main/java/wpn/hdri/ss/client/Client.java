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

import org.apache.log4j.Logger;
import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.engine.ReadAttributeTask;

import java.util.Map;

/**
 * Abstract base class for Tango or Tine client.
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 27.04.12
 */
public abstract class Client {
    /**
     * Fully qualified device name. E.g. for Tango: sys/tg_test/1
     */
    private final String deviceName;

    protected Client(String deviceName) {
        this.deviceName = deviceName;
    }

    protected String getDeviceName() {
        return deviceName;
    }

    /**
     * Polls attribute from server. Returns a {@link Map.Entry} where key is a value and value is a timestamp when this value
     * was set on server.
     * <p/>
     * So to acquire a value of "sys/tg_test/1/long_scalar_w" use the following code snippet:
     * <p/>
     * <code>
     * ClientFactory clientFactory = new ClientFactory();<br/>
     * Client client = clientFactory.createClient("sys/tg_test/1");<br/>
     * Map.Entry pair = client.readAttribute("long_scalar_w");<br/>
     * System.out.print(pair.getKey().toString() + "@" + pair.getValue().getValue());<br/>
     * </code>
     * This will produce smth like: 12345@13338780292
     *
     * @param attrName attribute name
     * @param <T>
     * @return (Value, Timestamp) pair
     * @throws ClientException
     */
    public abstract <T> Map.Entry<T, Timestamp> readAttribute(String attrName) throws ClientException;

    /**
     * Writes a value to an attribute.
     *
     * @param attrName attribute name
     * @param value    value
     * @param <T>      type of value
     * @throws ClientException if write failed
     */
    public abstract <T> void writeAttribute(String attrName, T value) throws ClientException;

    /**
     * Subscribes to attribute change event. When new value is available cbk#onRead will be called.
     * In case any error cbk#onError will be called and cause will be passed.
     *
     * @param attrName attribute
     * @param cbk      onRead, onError
     * @throws ClientException if subscription process failed
     */
    public abstract void subscribeEvent(String attrName, EventCallback cbk) throws ClientException;

    /**
     * Checks attribute. Most implementations will try to acquire an info from server.
     * If it fails an exception will be thrown.
     *
     * @param attrName attribute name to check
     */
    public abstract boolean checkAttribute(String attrName);

    /**
     * Returns appropriate Java type of the attribute's value, i.e. Integer or Long or whatever.
     *
     * @param attrName attribute name
     * @return Class
     * @throws ClientException in case of any error
     */
    public abstract Class<?> getAttributeClass(String attrName) throws ClientException;

    /**
     *
     * @param attrName
     * @return true if an attribute value is an array
     * @throws ClientException
     */
    public abstract boolean isArrayAttribute(String attrName) throws ClientException;

    /**
     * Unsubscribe from attribute change event, i.e. stop respond to new values from the attribute
     *
     * @param attrName attribute name
     * @throws ClientException if unsubscription process failed
     */
    public abstract void unsubscribeEvent(String attrName) throws ClientException;

    public abstract void printAttributeInfo(String name, Logger logger);
}
