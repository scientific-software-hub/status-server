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

import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.transform.Matcher;
import org.simpleframework.xml.transform.Transform;
import wpn.hdri.ss.data.Interpolation;
import wpn.hdri.ss.data.Method;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 26.04.12
 */
public class StatusServerConfigurationTest {

    private Serializer persister = new Persister(new Matcher() {
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

    @Test
    public void test() throws Exception {
        StatusServerConfiguration conf = persister.read(StatusServerConfiguration.class, StatusServerConfigurationTest.class.getResourceAsStream("/conf/StatusServer.test.xml"));

        assertEquals("StatusServer", conf.getServerName());
        assertEquals("Development", conf.getInstanceName());

        Device dev = conf.getDevices().get(0);

        assertEquals("Test.Device", dev.getName());

        DeviceAttribute attr0 = dev.getAttributes().get(0);
        assertEquals("Test.Attribute", attr0.getName());
        assertEquals(Method.POLL, attr0.getMethod());
        assertEquals(Interpolation.LINEAR, attr0.getInterpolation());
        assertEquals(100L, attr0.getDelay());
        assertEquals(new BigDecimal("0.5"), attr0.getPrecision());

        DeviceAttribute attr1 = dev.getAttributes().get(1);
        assertEquals("Test.Attribute.1", attr1.getName());
        assertEquals(Method.EVENT, attr1.getMethod());
        assertEquals(Interpolation.LAST, attr1.getInterpolation());
        assertEquals(0L, attr1.getDelay());
        assertSame(BigDecimal.ZERO, attr1.getPrecision());
    }

    @Test(expected = InvocationTargetException.class)
    public void test_bad() throws Exception {
        StatusServerConfiguration conf = persister.read(StatusServerConfiguration.class, StatusServerConfigurationTest.class.getResourceAsStream("/conf/StatusServer.test.BAD.xml"));
    }
}
