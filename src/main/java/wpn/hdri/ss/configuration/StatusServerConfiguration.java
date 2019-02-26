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

package wpn.hdri.ss.configuration;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.simpleframework.xml.*;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.transform.Matcher;
import org.simpleframework.xml.transform.Transform;
import wpn.hdri.ss.data.Method;

import java.io.*;
import java.util.List;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 26.04.12
 */
@Root(name = "StatusServer", strict = false)
public final class StatusServerConfiguration {
    public static final Serializer XML_SERIALIZER = new Persister(new Matcher() {
        @Override
        public Transform match(Class type) throws Exception {
            if (Method.class.isAssignableFrom(type)) {
                return new Transform<Method>() {
                    @Override
                    public Method read(String value) throws Exception {
                        return Method.valueOf(value.toUpperCase());
                    }

                    @Override
                    public String write(Method value) throws Exception {
                        return value.name();
                    }
                };
            }
            return null;
        }
    });

    @Attribute(name = "use-aliases")
    private boolean useAliases;
    @ElementList(name = "devices")
    private List<Device> devices;
    @ElementList(name = "attributes")
    private List<StatusServerAttribute> attributes;
    @Element(name = "properties")
    private StatusServerProperties properties;

    /**
     * Do not use this constructor directly. Use {@link ConfigurationBuilder} instead.
     *
     * @param useAliases
     * @param devices
     * @param attributes
     */
    public StatusServerConfiguration(
            @Attribute(name = "use-aliases") boolean useAliases,
            @ElementList(name = "devices") List<Device> devices,
            @ElementList(name = "attributes") List<StatusServerAttribute> attributes,
            @Element(name = "properties") StatusServerProperties properties) {
        this.useAliases = useAliases;
        this.devices = devices;
        this.attributes = attributes;
        this.properties = properties;
    }

    public static StatusServerConfiguration fromXml(String pathToXml) throws ConfigurationException {
        if (!new File(pathToXml).exists()) {
            throw new IllegalArgumentException(pathToXml + " does not exist.");
        }
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(pathToXml))){
            return XML_SERIALIZER.read(StatusServerConfiguration.class, bufferedReader);
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    public static StatusServerConfiguration fromXmlStream(InputStream xmlStream) throws ConfigurationException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(xmlStream))){
            return XML_SERIALIZER.read(StatusServerConfiguration.class, bufferedReader);
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    public boolean isUseAliases() {
        return useAliases;
    }

    public List<Device> getDevices() {
        return devices;
    }

    public List<StatusServerAttribute> getStatusServerAttributes() {
        return attributes;
    }

    public StatusServerProperties getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + hashCode() + "{" +
                "devices=" + devices +
                "}";
    }


    //TODO issue 6
    public DeviceAttribute getDeviceAttribute(final String deviceName, final String name) {
        return Iterables.filter(Iterables.filter(devices, new Predicate<Device>() {
            @Override
            public boolean apply(Device input) {
                return input.getName().equals(deviceName);
            }
        }).iterator().next().getAttributes(), new Predicate<DeviceAttribute>() {
            @Override
            public boolean apply(DeviceAttribute input) {
                return input.getName().equals(name);
            }
        }).iterator().next();
    }
}
