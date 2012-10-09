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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.transform.Matcher;
import org.simpleframework.xml.transform.Transform;
import wpn.hdri.ss.data.Interpolation;
import wpn.hdri.ss.data.Method;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 27.04.12
 */
public class ConfigurationBuilder {
    private final Serializer serializer = new Persister(new Matcher() {
        @Override
        public Transform match(Class type) throws Exception {
            if (Method.class.isAssignableFrom(type)) {
                return new Transform<Method>() {
                    @Override
                    public Method read(String value) throws Exception {
                        return Method.forAlias(value);
                    }

                    @Override
                    public String write(Method value) throws Exception {
                        return value.getAlias();
                    }
                };
            } else if (Interpolation.class.isAssignableFrom(type)) {
                return new Transform<Interpolation>() {
                    @Override
                    public Interpolation read(String value) throws Exception {
                        return Interpolation.forAlias(value);
                    }

                    @Override
                    public String write(Interpolation value) throws Exception {
                        return value.getAlias();
                    }
                };
            }
            return null;
        }
    });

    public ConfigurationBuilder() {
    }

    public StatusServerConfiguration fromXml(String pathToXml) throws ConfigurationException {
        if (!new File(pathToXml).exists()) {
            throw new IllegalArgumentException(pathToXml + " does not exist.");
        }
        try {
            return serializer.read(StatusServerConfiguration.class, new BufferedReader(new FileReader(pathToXml)));
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    private String serverName;
    private String instanceName;
    private boolean useAliases;

    public ConfigurationBuilder setServerName(String serverName, String instanceName) {
        this.serverName = Preconditions.checkNotNull(serverName);
        this.instanceName = Preconditions.checkNotNull(instanceName);
        return this;
    }

    public ConfigurationBuilder setUseAliases(boolean useAliases){
        this.useAliases = useAliases;
        return this;
    }

    private final List<String> devices = new ArrayList<String>();

    public ConfigurationBuilder addDevice(String deviceName) {
        devices.add(Preconditions.checkNotNull(deviceName));
        return this;
    }

    private final Multimap<String, DeviceAttribute> attributes = HashMultimap.create();

    public ConfigurationBuilder addAttributeToDevice(String deviceName, String attrName, String methodAlias,
                                                     String interpolationAlias, long delay, double precision) {
        if (!devices.contains(deviceName)) {
            addDevice(deviceName);
        }

        Method method = Method.forAlias(methodAlias);
        Interpolation interpolation = Interpolation.forAlias(interpolationAlias);


        DeviceAttribute attribute = new DeviceAttribute(attrName, null, method, interpolation, delay, BigDecimal.valueOf(precision));
        attributes.put(deviceName, attribute);
        return this;
    }

    public StatusServerConfiguration build() {
        return new StatusServerConfiguration(serverName, instanceName, useAliases,
                new ArrayList<Device>(
                        Collections2.<String, Device>transform(devices, new Function<String, Device>() {
                            @Override
                            public Device apply(String input) {
                                return new Device(input, new ArrayList<DeviceAttribute>(attributes.get(input)));
                            }
                        })));
    }
}
