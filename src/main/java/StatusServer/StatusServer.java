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

package StatusServer;

import com.google.common.collect.Multimap;
import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevState;
import fr.esrf.TangoDs.Attribute;
import fr.esrf.TangoDs.DeviceClass;
import fr.esrf.TangoDs.DeviceImpl;
import fr.esrf.TangoDs.WAttribute;
import org.apache.log4j.Logger;
import wpn.hdri.ss.Launcher;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.data.AttributeName;
import wpn.hdri.ss.data.AttributeValue;
import wpn.hdri.ss.data.AttributesView;
import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.engine.AttributeFilters;
import wpn.hdri.ss.engine.AttributesManager;
import wpn.hdri.ss.engine.ClientsManager;
import wpn.hdri.ss.engine.Engine;
import wpn.hdri.ss.storage.StorageFactory;
import wpn.hdri.tango.attribute.EnumAttrWriteType;
import wpn.hdri.tango.attribute.TangoAttribute;
import wpn.hdri.tango.attribute.TangoAttributeListener;
import wpn.hdri.tango.data.format.TangoDataFormat;
import wpn.hdri.tango.data.type.TangoDataType;
import wpn.hdri.tango.data.type.TangoDataTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 27.04.12
 */
public class StatusServer extends DeviceImpl {
    private final static Logger log = Logger.getLogger(StatusServer.class);

    private final int utilizedCpus;

    {
        int cpus;
        try {
            cpus = Integer.parseInt(System.getProperty(Launcher.CPUS_PROPERTY));
        } catch (NumberFormatException e) {
            log.error(Launcher.CPUS_PROPERTY + " property which defines number of cpu to be used is not set. Using default value=1");
            cpus = 1;
        }
        if (cpus < 1) {
            log.error("Number of cpu to be used can not be less then 1. Check " + Launcher.CPUS_PROPERTY + " property. Using default value=1");
            cpus = 1;
        }
        utilizedCpus = cpus;
    }

    private Engine engine;
    private Map<String, TangoAttribute<?>> attributes = new HashMap<String, TangoAttribute<?>>();

    public StatusServer(DeviceClass cl, String d_name, String de, DevState st, String sta) throws DevFailed {
        super(cl, d_name, de, st, sta);
        init_device();
    }

    public StatusServer(DeviceClass cl, String d_name) throws DevFailed {
        super(cl, d_name);
        init_device();
    }

    public StatusServer(DeviceClass cl, String d_name, String desc) throws DevFailed {
        super(cl, d_name, desc);
        init_device();
    }

    @Override
    public void init_device() throws DevFailed {
        get_logger().info("Init device:" + get_name());

        set_state(DevState.INIT);
    }

    public void postInit_device(StatusServerConfiguration configuration, StorageFactory storage, ClientsManager clientsManager, AttributesManager attributesManager) throws DevFailed {
        StatusServerAttribute.USE_ALIAS.<Boolean>toTangoAttribute().setCurrentValue(configuration.isUseAliases());

        this.engine = new Engine(clientsManager, attributesManager /*DEFAULT LOGGER*/, utilizedCpus);

        for (final wpn.hdri.ss.configuration.StatusServerAttribute attr : configuration.getStatusServerAttributes()) {
//            TangoDataType<?> type = TangoDataTypes.forString(attr.getType());
            TangoDataType<String> type = TangoDataTypes.forClass(String.class);
            TangoAttribute<?> attribute = new TangoAttribute<String>(
                    attr.getName(), TangoDataFormat.<String>createScalarDataFormat(), type, EnumAttrWriteType.WRITE, new TangoAttributeListener<String>() {
                @Override
                public String onLoad() {
                    throw new UnsupportedOperationException("This method is not supported in " + this.getClass());
                }

                @Override
                public void onSave(String value) {
                    String[] data_timestamp = value.split("@");

                    String data = data_timestamp[0];
                    Timestamp timestamp = new Timestamp(Long.parseLong(data_timestamp[1]));

                    engine.writeAttributeValue("/" + attr.getName(), data, timestamp);
                }
            });
            //write default value
            engine.writeAttributeValue("/" + attr.getName(), null, Timestamp.now());

            attributes.put(attr.getName(), attribute);
            add_attribute(attribute.toAttr());
        }

        set_state(DevState.ON);
    }

    //@Override
    public void delete_device() throws DevFailed {
        this.engine.shutdown();
        set_state(DevState.OFF);
    }

    @Override
    public Logger get_logger() {
        return log;
    }

    @Override
    public void init_logger() {
    }

    public Engine getEngine() {
        return engine;
    }

    public static StatusServer getInstance() {
        return (StatusServer) StatusServerClass.instance().get_device_at(0);
    }

    public boolean isUseAliases() {
        return StatusServerAttribute.USE_ALIAS.<Boolean>toTangoAttribute().getCurrentValue();
    }

    @Override
    public void read_attr(Attribute attribute) throws DevFailed {
        String name = attribute.get_name();
        if (name.equalsIgnoreCase(StatusServerAttribute.USE_ALIAS.toTangoAttribute().getName())) {
            StatusServerAttribute.USE_ALIAS.<Boolean>toTangoAttribute().read(attribute);
        } else if (name.equalsIgnoreCase(StatusServerAttribute.CURRENT_ACTIVITY.toTangoAttribute().getName())) {
            attribute.set_value(engine.getCurrentActivity());
        } else if (name.equalsIgnoreCase(StatusServerAttribute.TIMESTAMP.toTangoAttribute().getName())) {
            attribute.set_value(System.currentTimeMillis());
        } else if (name.equalsIgnoreCase(StatusServerAttribute.DATA_ENCODED.toTangoAttribute().getName())) {
            //TODO call StatusServerCommand#GET_DATA_ENCODED instead
            Multimap<AttributeName, AttributeValue<?>> data = engine.getAllAttributeValues(null, AttributeFilters.none());

            AttributesView view = new AttributesView(data, isUseAliases());

            String result = view.toJsonString();
            //TODO encode in Base64
            attribute.set_value(result);
        }
    }

    @Override
    public void write_attr_hardware(Vector attr_list) throws DevFailed {
        for (int i = 0; i < attr_list.size(); i++) {
            WAttribute att = dev_attr.get_w_attr_by_ind(((Integer) (attr_list.elementAt(i))).intValue());
            String attr_name = att.get_name();

            if (attr_name.equalsIgnoreCase(StatusServerAttribute.USE_ALIAS.toTangoAttribute().getName())) {
                StatusServerAttribute.USE_ALIAS.<Boolean>toTangoAttribute().write(att);
            } else {
                if (attributes.containsKey(attr_name)) {
                    attributes.get(attr_name).write(att);
                }
            }
        }
    }
}
