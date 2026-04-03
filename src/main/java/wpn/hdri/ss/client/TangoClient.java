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

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.DeviceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tango.client.ez.proxy.*;
import org.tango.client.ez.util.TangoUtils;
import org.tango.utils.DevFailedUtils;
import wpn.hdri.ss.client2.ClientAdaptor;
import wpn.hdri.ss.data.Method;
import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.engine2.EventTask;

import java.net.URI;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 27.04.12
 */
public class TangoClient extends Client implements ClientAdaptor {
    private final Logger logger = LoggerFactory.getLogger(TangoClient.class);
    private static final EnumMap<Method.EventType, Object> TANGO_EVENT_TYPES = new EnumMap<Method.EventType, Object>(Method.EventType.class);

    static {
        TANGO_EVENT_TYPES.put(Method.EventType.CHANGE, TangoEvent.CHANGE);
        TANGO_EVENT_TYPES.put(Method.EventType.PERIODIC, TangoEvent.PERIODIC);
    }

    private AtomicReference<TangoProxy> proxy = new AtomicReference<>(null);
    private Map<String, TangoEventListener<?>> listeners = new HashMap<>();

    public TangoClient(URI deviceName) {
        super(deviceName.toString());
    }

    protected EnumMap<Method.EventType, Object> mapEventTypes() {
        return TANGO_EVENT_TYPES;
    }

    @Override
    public Class<?> getAttributeClass(String attrName) throws ClientException {
        try {
            this.proxy.compareAndSet(null,
                    TangoProxies.newDeviceProxyWrapper(new DeviceProxy(getDeviceName())));

            TangoAttributeInfoWrapper attributeInfo = proxy.get().getAttributeInfo(attrName);
            if (attributeInfo == null)
                throw new ClientException("Exception in " + proxy.get().getName(), new NullPointerException("attributeInfo is null"));
            return attributeInfo.getClazz();
        } catch (TangoProxyException | NoSuchAttributeException e) {
            throw new ClientException("Exception in " + getDeviceName(), e, classifyTangoProxyException(e));
        } catch (DevFailed devFailed) {
            throw new ClientException("DevFailed: " + firstReason(devFailed),
                    TangoUtils.convertDevFailedToException(devFailed), classifyDevFailed(devFailed));
        }
    }

    @Override
    public <T> SingleRecord<T> read(Attribute<T> attr) throws ClientException {
        try {
            this.proxy.compareAndSet(null,
                    TangoProxies.newDeviceProxyWrapper(new DeviceProxy(getDeviceName())));

            ValueTime<?> value = proxy.get().readAttributeValueAndTime(attr.name);
            return new SingleRecord<>(attr, System.currentTimeMillis(), value.getTime(), (T)value.getValue());
        } catch(ReadAttributeException e) {
            String detail = e.reason + ": " + e.desc;
            ClientException.FailureType type = e.devFailed != null
                    ? classifyDevFailed(e.devFailed)
                    : ClientException.FailureType.DEVICE_ERROR;
            throw new ClientException(detail, e, type);
        } catch (TangoProxyException | NoSuchAttributeException e) {
            throw new ClientException(e.getMessage(), e, classifyTangoProxyException(e));
        } catch (DevFailed devFailed) {
            throw new ClientException("DevFailed: " + firstReason(devFailed),
                    TangoUtils.convertDevFailedToException(devFailed), classifyDevFailed(devFailed));
        }
    }

    @Override
    public void subscribe(final EventTask cbk) {
        final Attribute attr = cbk.getAttribute();
        try {
            this.proxy.compareAndSet(null,
                    TangoProxies.newDeviceProxyWrapper(new DeviceProxy(getDeviceName())));

            proxy.get().subscribeToEvent(attr.name, (TangoEvent) eventTypesMap.get(attr.eventType));
            TangoEventListener<Object> listener = new TangoEventListener<Object>() {
                @Override
                public void onEvent(org.tango.client.ez.proxy.EventData<Object> data) {
                    SingleRecord<?> record = new SingleRecord<>(attr, System.currentTimeMillis(), data.getTime(), data.getValue());
                    cbk.onEvent(record);
                }

                @Override
                public void onError(Exception cause) {
                    logger.warn("{}/{}: {}", getDeviceName(), attr.name, cause.getMessage());
                    cbk.onError(cause);
                }
            };
            proxy.get().addEventListener(attr.name, (TangoEvent) eventTypesMap.get(attr.eventType), listener);

            listeners.put(attr.name, listener);
        } catch (TangoProxyException | NoSuchAttributeException e) {
            logger.error("Failed to subscribe to {}/{}: {}", getDeviceName(), attr.name, e.getMessage());
            cbk.onError(new ClientException("Subscribe failed for " + getDeviceName() + "/" + attr.name,
                    e, classifyTangoProxyException(e)));
        } catch (DevFailed devFailed) {
            DevFailedUtils.logDevFailed(devFailed, logger);
            cbk.onError(new ClientException("Subscribe failed for " + getDeviceName() + "/" + attr.name,
                    TangoUtils.convertDevFailedToException(devFailed), classifyDevFailed(devFailed)));
        }
    }

    // --- classification helpers ---

    private static String firstReason(DevFailed df) {
        return (df.errors != null && df.errors.length > 0) ? df.errors[0].reason : "unknown";
    }

    private static ClientException.FailureType classifyDevFailed(DevFailed df) {
        String reason = firstReason(df).toLowerCase();
        if (reason.contains("devicenotexported") || reason.contains("device_not_exported")) {
            return ClientException.FailureType.DEVICE_NOT_EXPORTED;
        }
        if (reason.contains("cantconnect") || reason.contains("cant_connect")
                || reason.contains("corbaexception") || reason.contains("corba_exception")
                || reason.contains("connection refused")) {
            return ClientException.FailureType.CONNECTION_REFUSED;
        }
        return ClientException.FailureType.DEVICE_ERROR;
    }

    private static ClientException.FailureType classifyTangoProxyException(Exception e) {
        // TangoProxyException (and its subclasses) expose a structured reason field
        if (e instanceof TangoProxyException tpe && tpe.devFailed != null) {
            return classifyDevFailed(tpe.devFailed);
        }
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (msg.contains("connection refused") || msg.contains("cantconnect")
                || msg.contains("unreachable") || msg.contains("corba")) {
            return ClientException.FailureType.CONNECTION_REFUSED;
        }
        return ClientException.FailureType.OTHER;
    }

    @Override
    public void unsubscribe(Attribute attr) {
        listeners.remove(attr.name);
        try {
            this.proxy.compareAndSet(null,
                    TangoProxies.newDeviceProxyWrapper(new DeviceProxy(getDeviceName())));

            proxy.get().unsubscribeFromEvent(attr.name, (TangoEvent) eventTypesMap.get(attr.eventType));
        } catch (TangoProxyException devFailed) {
            logger.error(devFailed.toString());
        } catch (DevFailed devFailed) {
            DevFailedUtils.logDevFailed(devFailed, logger);
        }

    }
}
