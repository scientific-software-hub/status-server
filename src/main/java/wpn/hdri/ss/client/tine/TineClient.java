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

package wpn.hdri.ss.client.tine;

import de.desy.tine.client.TCallback;
import de.desy.tine.client.TLink;
import de.desy.tine.dataUtils.TDataType;
import de.desy.tine.definitions.TAccess;
import de.desy.tine.definitions.TErrorList;
import de.desy.tine.definitions.TMode;
import de.desy.tine.queryUtils.TPropertyQuery;
import de.desy.tine.queryUtils.TQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tango.client.ez.proxy.EventData;
import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.client.ClientException;
import wpn.hdri.ss.client.EventCallback;
import wpn.hdri.ss.client2.ClientAdaptor;
import wpn.hdri.ss.data.Method;
import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.engine2.EventTask;

import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * This instance stores a number of {@link TLink} and reads data from them.
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 27.04.12
 */
public class TineClient extends Client implements ClientAdaptor {
    private static final EnumMap<Method.EventType, Object> TINE_EVENT_TYPES = new EnumMap<Method.EventType, Object>(Method.EventType.class);

    static {
        TINE_EVENT_TYPES.put(Method.EventType.CHANGE, TMode.CM_DATACHANGE);
        TINE_EVENT_TYPES.put(Method.EventType.PERIODIC, TMode.CM_TIMER);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TineClient.class);
    private final String context;
    private final String serverName;
    private final String deviceName;
    private ConcurrentMap<String, Future<TLink>> tlinks = new ConcurrentHashMap<String, Future<TLink>>();

    public TineClient(String deviceName) {
        super(deviceName);
        String[] deviceInfo = deviceName.split("/");
        this.context = deviceInfo[1];//skip leading '/'
        this.serverName = deviceInfo[2];
        this.deviceName = deviceInfo[3];
    }

    protected EnumMap<Method.EventType, Object> mapEventTypes() {


        return TINE_EVENT_TYPES;
    }

    /**
     * Implementation stores {@link TLink} instances wrapped by {@link FutureTask} in private {@link this#tlinks} field.
     * This is done to prevent multiply dummy TLink instances creation.
     *
     * @param attrName
     * @return
     * @throws ClientException
     */
    @Override
    public <T> Map.Entry<T, Timestamp> readAttribute(final String attrName) throws ClientException {
        Future<TLink> futureLink = getFutureLink(attrName);
        try {
            TLink tLink = futureLink.get();
            int rc = tLink.execute();
            if (rc != TErrorList.success) {
                throw new RuntimeException("TLink has failed: " + TErrorList.getErrorString(rc));
            }
            TDataType dout = tLink.dOutput;
            long time = tLink.getLastTimeStamp();
            Timestamp timestamp = new Timestamp(time);
            return new AbstractMap.SimpleImmutableEntry<T, wpn.hdri.ss.data.Timestamp>((T) getDataObject(dout), timestamp);
        } catch (Exception e) {
            throw new ClientException("Read from " + getDeviceName() + "/" + attrName + " failed.", e);
        }
    }

    private Future<TLink> getFutureLink(final String attrName) {
        Future<TLink> futureLink = tlinks.get(attrName);
        if (futureLink == null) {
            FutureTask<TLink> ft =
                    new FutureTask<TLink>(
                            new Callable<TLink>() {
                                @Override
                                public TLink call() throws Exception {
                                    TPropertyQuery meta = getTPropertyQuery(attrName);
                                    int size = getTPropertySize(meta);
                                    short dataFormat = getTPropertyFormat(meta);
                                    TDataType dout = new TDataType(size, dataFormat);
                                    // get a reference array : synchronous call ...
                                    TLink result = new TLink(getDeviceName(), attrName, dout, null, TAccess.CA_READ);
                                    return result;
                                }
                            });


            futureLink = tlinks.putIfAbsent(attrName, ft);
            if (futureLink == null) {
                ft.run();
                futureLink = ft;
            }
        }
        return futureLink;
    }

    /**
     * @param dout
     * @return Object or Object[]
     */
    private Object getDataObject(TDataType dout) {
        int length = Array.getLength(dout.getDataObject());
        Object dataObject = dout.getDataObject();
        //TODO it appears that TINE always returns array of values
        //TODO and the only interesting value is always the first one in this array
        //if (length > 1) {
        //    return dataObject;
        //} else {
        return Array.get(dataObject, 0);
        //}
    }

    @Override
    public void subscribeEvent(final String attrName, Method.EventType type, final EventCallback cbk) throws ClientException {
        Future<TLink> futureLink = getFutureLink(attrName);
        try {
            final TLink link = futureLink.get();
            final TDataType dout = link.dOutput;
            long time = link.getLastTimeStamp();
            //read data for the first time
            cbk.onEvent(new EventData<Object>(getDataObject(dout), time));
            //attach event listener
            int rc = link.attach((Short) eventTypesMap.get(type), new TCallback() {
                @Override
                public void callback(int LinkIndex, int LinkStatus) {
                    if (TErrorList.isLinkSuccess(LinkStatus)) {
                        long time = link.getLastTimeStamp();
                        cbk.onEvent(new EventData<Object>(getDataObject(dout), time));
                    } else {
                        cbk.onError(new Exception(TErrorList.getErrorString(LinkStatus)));
                    }
                }
            });
            if (rc < 0) {
                throw new IllegalStateException(link.getLastError());
            }
        } catch (Exception e) {
            throw new ClientException("Failed subscribe to " + getDeviceName() + "/" + attrName, e);
        }
    }

    @Override
    public boolean checkAttribute(String attrName) {
        try {
            getTPropertyQuery(attrName);
            return true;
        } catch (ClientException e) {
            return false;
        }
    }

    private int getTPropertySize(TPropertyQuery meta) {
        int result = meta.prpSize;
        return result;
    }

    private short getTPropertyFormat(TPropertyQuery meta) {
        short result = meta.prpFormat;
        return result;
    }

    @Override
    public Class<?> getAttributeClass(String attrName) throws ClientException {
        TPropertyQuery meta = getTPropertyQuery(attrName);
        int size = getTPropertySize(meta);
        short dataFormat = getTPropertyFormat(meta);
        TDataType dataType = new TDataType(size, dataFormat);
        //TODO it appears that TINE always returns array of values
        //TODO and the only interesting value is always the first one in this array
        //if (size > 1) {
//            return dataType.getDataObject().getClass();
        //      } else {
        return dataType.getDataObject().getClass().getComponentType();
        //    }
    }

    @Override
    public boolean isArrayAttribute(String attrName) throws ClientException {
        //see comment in getAttributeClass
        return false;
    }

    @Override
    public void unsubscribeEvent(String attrName) throws ClientException {
        Future<TLink> futureLink = tlinks.remove(attrName);
        try {
            final TLink link = futureLink.get();
            int rc = link.close();
            if (rc < 0) {
                throw new ClientException(link.getLastError(), null);
            }
        } catch (Exception e) {
            throw new ClientException("Failed subscribe to " + getDeviceName() + "/" + attrName, e);
        }
    }

    private TPropertyQuery getTPropertyQuery(String attrName) throws ClientException {
        TPropertyQuery[] result = TQuery.getPropertyInformation(context, serverName, deviceName, attrName);
        if (result == null) {
            throw new ClientException("Cannot read meta info for " + getDeviceName() + "/" + attrName, new NullPointerException());
        }
        return result[0];
    }

    @Override
    public void printAttributeInfo(String name) {
        try {
            LOGGER.info("Information for attribute " + deviceName);
            LOGGER.info("Data format: TINE data format");
            LOGGER.info("Data type: TINE data type");
            LOGGER.info("Java data type match:" + getAttributeClass(name).getSimpleName());
        } catch (ClientException e) {
            LOGGER.warn("Can not print attribute info for " + name, e);
        }
    }


    @Override
    public <T> SingleRecord<T> read(Attribute<T> attr) throws ClientException {
        Future<TLink> futureLink = getFutureLink(attr.name);
        try {
            TLink tLink = futureLink.get();
            int rc = tLink.execute();
            if (rc != TErrorList.success) {
                throw new Exception("TLink has failed: " + TErrorList.getErrorString(rc));
            }
            TDataType dout = tLink.dOutput;
            long time = tLink.getLastTimeStamp();
            return new SingleRecord<T>(attr, System.currentTimeMillis(), time, (T)getDataObject(dout));
        } catch (Exception e) {
            throw new ClientException("Read from " + getDeviceName() + "/" + attr.name + " has failed:" + e.getMessage(), e);
        }
    }

    @Override
    public void subscribe(final EventTask eventTask) {
        final Attribute attr = eventTask.getAttribute();
        Future<TLink> futureLink = getFutureLink(attr.name);
        try {
            final TLink link = futureLink.get();
            final TDataType dout = link.dOutput;
            long time = link.getLastTimeStamp();
            //read data for the first time
            SingleRecord<?> record = new SingleRecord<>(attr,System.currentTimeMillis(),time,getDataObject(dout));
            eventTask.onEvent(record);
            //attach event listener
            int rc = link.attach((Short) eventTypesMap.get(attr.eventType), new TCallback() {
                @Override
                public void callback(int LinkIndex, int LinkStatus) {
                    if (TErrorList.isLinkSuccess(LinkStatus)) {
                        long time = link.getLastTimeStamp();
                        SingleRecord<?> record = new SingleRecord<>(attr,System.currentTimeMillis(),time,getDataObject(dout));
                        eventTask.onEvent(record);
                    } else {
                        LOGGER.error(TErrorList.getErrorString(LinkStatus));
                    }
                }
            });
            if (rc < 0) {
                throw new IllegalStateException(link.getLastError());
            }
        } catch (Exception e) {
            LOGGER.error("Failed subscribe to " + getDeviceName() + "/" + attr.name + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void unsubscribe(Attribute attr) {
        Future<TLink> futureLink = tlinks.remove(attr.name);
        try {
            final TLink link = futureLink.get();
            int rc = link.close();
            if (rc < 0) {
                throw new IllegalStateException(link.getLastError());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to unsubscribe from " + getDeviceName() + "/" + attr.name + ": " + e.getMessage(), e);
        }
    }
}
