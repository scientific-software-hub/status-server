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

package wpn.hdri.ss.data;

import org.junit.Test;

import static junit.framework.Assert.assertSame;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 27.07.12
 */
public class ValueTest {
    @Test
    public void testGetInstance_String() throws Exception {
        Object data1 = "Some value";
        Object data2 = new String("Some value");

        Value v1 = Value.getInstance(data1);
        Value v2 = Value.getInstance(data2);

        assertSame(v1, v2);
    }

    @Test
    public void testGetInstance_Integer() throws Exception {
        int data1 = 123;
        Object data2 = new Integer(123);

        Value v1 = Value.getInstance(data1);
        Value v2 = Value.getInstance(data2);

        assertSame(v1, v2);
    }

    @Test
    public void testGetInstance_Double() throws Exception {
        double data1 = 123.;
        Object data2 = new Double(123.);

        Value v1 = Value.getInstance(data1);
        Value v2 = Value.getInstance(data2);

        assertSame(v1, v2);
    }

    /**
     * This test demonstrates how big values of double adjusted by a small value can lose precision
     *
     * @throws Exception
     */
    @Test
    public void testGetInstance_BigDouble() throws Exception {
        double data1 = 12345678999999.;
        Object data2 = new Double(12345678999999. + 0.0005);

        Value v1 = Value.getInstance(data1);
        Value v2 = Value.getInstance(data2);

        assertSame(v1, v2);//should fail but it does not
    }
}
