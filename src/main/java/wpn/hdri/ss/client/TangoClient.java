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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tango.client.ez.proxy.*;
import wpn.hdri.ss.client2.ClientAdaptor;
import wpn.hdri.ss.data.Method;
import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.engine2.EventTask;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 27.04.12
 */
public class TangoClient extends Client implements ClientAdaptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TangoClient.class);
    private static final EnumMap<Method.EventType, Object> TANGO_EVENT_TYPES = new EnumMap<Method.EventType, Object>(Method.EventType.class);

    static {
        TANGO_EVENT_TYPES.put(Method.EventType.CHANGE, TangoEvent.CHANGE);
        TANGO_EVENT_TYPES.put(Method.EventType.PERIODIC, TangoEvent.PERIODIC);
    }
    private final TangoProxy proxy;
    private Map<String, TangoEventListener<?>> listeners = new HashMap<>();

    public TangoClient(String deviceName, TangoProxy proxy) {
        super(deviceName);
        this.proxy = proxy;
    }

    protected EnumMap<Method.EventType, Object> mapEventTypes() {
        return TANGO_EVENT_TYPES;
    }

    @Override
    public Class<?> getAttributeClass(String attrName) throws ClientException {
        try {
            TangoAttributeInfoWrapper attributeInfo = proxy.getAttributeInfo(attrName);
            if (attributeInfo == null)
                throw new ClientException("Exception in " + proxy.getName(), new NullPointerException("attributeInfo is null"));
            return attributeInfo.getClazz();
        } catch (TangoProxyException | NoSuchAttributeException e) {
            throw new ClientException("Exception in " + proxy.getName(), e);
        }
    }

    @Override
    public <T> SingleRecord<T> read(Attribute<T> attr) throws ClientException {
        try {
            ValueTime<?> value = proxy.readAttributeValueAndTime(attr.name);
            return new SingleRecord<>(attr, System.currentTimeMillis(), value.getTime(), (T)value.getValue());
        } catch (TangoProxyException|NoSuchAttributeException e) {
            throw new ClientException(e.getMessage(),e);
        }
    }

    @Override
    public void subscribe(final EventTask cbk) {
        final Attribute attr = cbk.getAttribute();
        try {
            proxy.subscribeToEvent(attr.name, (TangoEvent) eventTypesMap.get(attr.eventType));
            TangoEventListener<Object> listener = new TangoEventListener<Object>() {
                @Override
                public void onEvent(EventData<Object> data) {
                    SingleRecord<?> record = new SingleRecord<>(attr, System.currentTimeMillis(), data.getTime(), data.getValue());
                    cbk.onEvent(record);
                }

                @Override
                public void onError(Exception cause) {
                    LOGGER.error(cause.getMessage(), cause);
                }
            };
            proxy.addEventListener(attr.name, (TangoEvent) eventTypesMap.get(attr.eventType), listener);

            listeners.put(attr.name, listener);
        } catch (TangoProxyException | NoSuchAttributeException devFailed) {
            LOGGER.error(devFailed.toString());
        }
    }

    @Override
    public void unsubscribe(Attribute attr) {
        listeners.remove(attr.name);
        try {
            proxy.unsubscribeFromEvent(attr.name, (TangoEvent) eventTypesMap.get(attr.eventType));
        } catch (TangoProxyException devFailed) {
            LOGGER.warn(devFailed.toString());
        }
    }
}
