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

    //TODO move following tests to integration tests as they are environment depended
    @Test
    public void testReadImageAttr() throws Exception {
        ClientFactory factory = new ClientFactory();
        Client client = factory.createClient("sys/tg_test/1");

        client.<float[][]>writeAttribute("float_image", new float[][]{{0.1F, 0.3F}, {0.2F, 0.4F}, {0.5F, 0.7F}});

        Map.Entry<float[][], Timestamp> value = client.<float[][]>readAttribute("float_image");
        float[][] result = value.getKey();

        assertArrayEquals(new float[]{0.1F, 0.3F}, result[0], 0.01F);
        assertArrayEquals(new float[]{0.2F, 0.4F}, result[1], 0.01F);
        assertArrayEquals(new float[]{0.5F, 0.7F}, result[2], 0.01F);
    }

    @Test(expected = ClientException.class)
    public void testReadAttr_Failed() throws Exception {
        ClientFactory factory = new ClientFactory();
        Client client = factory.createClient("sys/tg_test/1");

        client.readAttribute("throw_exception");
    }

    @Test
    public void testReadSpectrumAttr() throws Exception {
        ClientFactory factory = new ClientFactory();
        Client client = factory.createClient("sys/tg_test/1");

        client.writeAttribute("double_spectrum", new double[]{0.1D, 0.9D, 0.8D, 0.4D});

        double[] result = client.<double[]>readAttribute("double_spectrum").getKey();

        assertArrayEquals(new double[]{0.1D, 0.9D, 0.8D, 0.4D}, result, 0.0D);
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

    //@Test
    public void testWriteAttribute() throws Exception {
        ClientFactory factory = new ClientFactory();
        Client client = factory.createClient("production/json/0");
        assertNotNull(client);
        client.writeAttribute("CRT_USER_NAME", "khokhria");

        String result = client.<String>readAttribute("CRT_USER_NAME").getKey();

        assertEquals("khokhria", result);
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
