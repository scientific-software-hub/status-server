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

import de.desy.tine.types.NAME64;
import org.junit.Test;
import wpn.hdri.ss.data.Timestamp;

import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 08.05.12
 */
public class TineClientTest {

    private final String deviceName = "/TEST/SineServer/#SineGen3";

    @Test
    public void testReadAttribute() throws Exception {
        TineClient instance = new TineClient(deviceName);

        Map.Entry<Object, Timestamp> result = instance.readAttribute("MessageText");

        assertEquals("Don't worry, be happy!", result.getKey().toString());
    }

    @Test
    public void testSubscribeEvent() throws Exception {

    }

    @Test
    public void testCheckAttribute() throws Exception {

    }

    @Test
    public void testGetAttributeClass() throws Exception {
        TineClient instance = new TineClient(deviceName);

        Class<?> result = instance.getAttributeClass("MessageText");

        //assertSame(String.class,result);
        assertSame(NAME64.class, result);
    }
}
