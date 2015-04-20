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

package wpn.hdri.ss.client.tango;

import org.apache.log4j.Logger;
import org.tango.client.ez.data.format.SpectrumTangoDataFormat;
import org.tango.client.ez.proxy.*;
import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.client.ClientException;
import wpn.hdri.ss.client.EventCallback;
import wpn.hdri.ss.data.Timestamp;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 27.04.12
 */
@NotThreadSafe
public class TangoClient extends Client {
    private final TangoProxy proxy;
    private Map<String, TangoEventListener<?>> listeners = new HashMap<>();

    public TangoClient(String deviceName, TangoProxy proxy) {
        super(deviceName);
        this.proxy = proxy;
    }

    /**
     * Reads value and time of the attribute specified by name
     *
     * @param attrName attribute name
     * @param <T>
     * @return
     * @throws ClientException
     */
    @Override
    public <T> Map.Entry<T, Timestamp> readAttribute(String attrName) throws ClientException {
        try {
            Map.Entry<T, Long> entry = proxy.readAttributeValueAndTime(attrName);
            return new AbstractMap.SimpleImmutableEntry<T, Timestamp>(entry.getKey(), new Timestamp(entry.getValue()));
        } catch (TangoProxyException devFailed) {
            throw new ClientException("Exception in " + proxy.getName(), devFailed);
        }
    }

    @Override
    public boolean isArrayAttribute(String attrName) throws ClientException {
        try {
            TangoAttributeInfoWrapper attributeInfo = proxy.getAttributeInfo(attrName);
            if (attributeInfo == null)
                throw new ClientException("Can not execute query!", new NullPointerException("attributeInfo is null"));
            return SpectrumTangoDataFormat.class.isAssignableFrom(attributeInfo.getFormat().getClass());
        } catch (TangoProxyException e) {
            throw new ClientException("Can not execute query!", e);
        }
    }

    @Override
    public void subscribeEvent(final String attrName, final EventCallback cbk) throws ClientException {
        try {
            proxy.subscribeToEvent(attrName, TangoEvent.CHANGE);
            TangoEventListener<Object> listener = new TangoEventListener<Object>() {
                @Override
                public void onEvent(EventData<Object> data) {
                    cbk.onEvent(new wpn.hdri.ss.client.EventData(data.getValue(), data.getTime()));
                }

                @Override
                public void onError(Exception cause) {
                    cbk.onError(cause);
                }
            };
            proxy.addEventListener(attrName, TangoEvent.CHANGE, listener);

            listeners.put(attrName, listener);
        } catch (TangoProxyException devFailed) {
            throw new ClientException("Exception in " + proxy.getName(), devFailed);
        }
    }

    /**
     * @param attrName attribute name to check
     * @return true if attribute is ok, false otherwise
     */
    @Override
    public boolean checkAttribute(String attrName) {
        try {
            return proxy.hasAttribute(attrName);
        } catch (TangoProxyException e) {
            return false;
        }
    }

    @Override
    public Class<?> getAttributeClass(String attrName) throws ClientException {
        try {
            TangoAttributeInfoWrapper attributeInfo = proxy.getAttributeInfo(attrName);
            if (attributeInfo == null)
                throw new ClientException("Exception in " + proxy.getName(), new NullPointerException("attributeInfo is null"));
            return attributeInfo.getClazz();
        } catch (TangoProxyException e) {
            throw new ClientException("Exception in " + proxy.getName(), e);
        }
    }

    @Override
    public void unsubscribeEvent(String attrName) throws ClientException {
        listeners.remove(attrName);
        try {
            proxy.unsubscribeFromEvent(attrName, TangoEvent.CHANGE);
        } catch (TangoProxyException devFailed) {
            throw new ClientException("Can not unsubscribe event for " + attrName, devFailed);
        }
    }

    @Override
    public void printAttributeInfo(String name, Logger logger) {
        TangoAttributeInfoWrapper info = null;
        try {
            info = proxy.getAttributeInfo(name);
            logger.info("Information for attribute " + proxy.getName() + "/" + name);
            logger.info("Data format:" + info.getFormat().toString());
            logger.info("Data type:" + info.getType().toString());
            logger.info("Java data type match:" + info.getClazz().getSimpleName());
        } catch (TangoProxyException e) {
            logger.warn("Can not print attribute info for " + name + ". Reason - info is null.");
        }
    }
}
