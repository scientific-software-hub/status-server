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

import org.junit.Test;
import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.client.ClientException;
import wpn.hdri.ss.client.ClientFactory;
import wpn.hdri.ss.data.Timestamp;

import java.util.Map;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertArrayEquals;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 04.05.12
 */
public class ITTangoClientTest {
    private final String name = "Test.Device";

    @Test(expected = ClientException.class)
    public void testReadAttr_Failed() throws Exception {
        ClientFactory factory = new ClientFactory();
        Client client = factory.createClient("sys/tg_test/1");

        client.readAttribute("throw_exception");
    }

    //@Test
    public void testReadAttr_Position() throws Exception {
        ClientFactory factory = new ClientFactory();
        Client client = factory.createClient("tango://hasgksspp07oh1.desy.de:10000/p07/dcmmotor/dcm_2nd_yaw");
        for (int i = 0; i < 1000; i++) {
            Map.Entry<Double, Timestamp> result = client.<Double>readAttribute("Position");
            System.out.println(result.getKey());
        }
    }

    @Test
    public void testCheckAttribute_Success() {
        ClientFactory factory = new ClientFactory();
        Client client = factory.createClient("sys/tg_test/1");

        boolean result = client.checkAttribute("double_scalar_w");

        assertTrue(result);
    }

    @Test
    public void testCheckAttribute_Failure() {
        ClientFactory factory = new ClientFactory();
        Client client = factory.createClient("sys/tg_test/1");

        boolean result = client.checkAttribute("double_scalar_wXXX");

        assertFalse(result);
    }
}
