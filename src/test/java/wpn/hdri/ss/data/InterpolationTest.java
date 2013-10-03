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

import org.junit.Before;
import org.junit.Test;
import wpn.hdri.ss.data.attribute.AttributeValue;
import wpn.hdri.ss.data.attribute.AttributeValueFactory;

import static junit.framework.Assert.assertEquals;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 27.04.12
 */
public class InterpolationTest {
    private long timestamp;
    private String fullName = "test_Attribute";

    @Before
    public void before() {
        timestamp = System.currentTimeMillis();
    }

    @Test
    public void test_Last() {
        AttributeValue<String> left = AttributeValueFactory.newAttributeValue(fullName, null, Value.<String>getInstance("Hello World!"), new Timestamp(timestamp), new Timestamp(timestamp));
        String result = Interpolation.LAST.interpolate(left, null, new Timestamp(timestamp + 10)).getValue().get();

        assertEquals("Hello World!", result);
    }

    @Test
    public void test_NearestFloor() {
        AttributeValue<String> ceil = AttributeValueFactory.newAttributeValue(fullName, null, Value.<String>getInstance("Hello World!"), new Timestamp(timestamp), new Timestamp(timestamp));
        AttributeValue<String> floor = AttributeValueFactory.newAttributeValue(fullName, null, Value.<String>getInstance("Hello World, again!"), new Timestamp(timestamp + 10), new Timestamp(timestamp + 10));

        String result = Interpolation.NEAREST.interpolate(ceil, floor, new Timestamp(timestamp + 7)).getValue().get();

        assertEquals("Hello World, again!", result);
    }

    @Test
    public void test_NearestCeil() {
        AttributeValue<String> ceil = AttributeValueFactory.newAttributeValue(fullName, null, Value.<String>getInstance("Hello World!"), new Timestamp(timestamp), new Timestamp(timestamp));
        AttributeValue<String> floor = AttributeValueFactory.newAttributeValue(fullName, null, Value.<String>getInstance("Hello World, again!"), new Timestamp(timestamp + 10), new Timestamp(timestamp + 10));

        String result = Interpolation.NEAREST.interpolate(ceil, floor, new Timestamp(timestamp + 3)).getValue().get();

        assertEquals("Hello World!", result);
    }

    @Test
    public void test_NearestBetween() {
        AttributeValue<String> ceil = AttributeValueFactory.newAttributeValue(fullName, null, Value.<String>getInstance("Hello World!"), new Timestamp(timestamp), new Timestamp(timestamp));
        AttributeValue<String> floor = AttributeValueFactory.newAttributeValue(fullName, null, Value.<String>getInstance("Hello World, again!"), new Timestamp(timestamp + 10), new Timestamp(timestamp + 10));

        String result = Interpolation.NEAREST.interpolate(ceil, floor, new Timestamp(timestamp + 5)).getValue().get();

        assertEquals("Hello World, again!", result);
    }

    @Test
    public void test_Linear() {
        AttributeValue<Double> ceil = AttributeValueFactory.newAttributeValue(fullName, null, Value.getInstance(0.), new Timestamp(0L), new Timestamp(0L));
        AttributeValue<Double> floor = AttributeValueFactory.newAttributeValue(fullName, null, Value.getInstance(10.), new Timestamp(10L), new Timestamp(10L));

        double result = Interpolation.LINEAR.interpolate(ceil, floor, new Timestamp(7)).getValue().get();

        assertEquals(7., result);
    }

    @Test
    public void test_Linear_Integer() {
        AttributeValue<Integer> ceil = AttributeValueFactory.newAttributeValue(fullName, null, Value.getInstance(0), new Timestamp(0L), new Timestamp(0L));
        AttributeValue<Integer> floor = AttributeValueFactory.newAttributeValue(fullName, null, Value.getInstance(10), new Timestamp(10L), new Timestamp(10L));

        int result = Interpolation.LINEAR.interpolate(ceil, floor, new Timestamp(7)).getValue().get();

        assertEquals(7, result);
    }

    @Test
    public void test_Linear_CornerLeft() {
        AttributeValue<Double> ceil = AttributeValueFactory.newAttributeValue(fullName, null, Value.getInstance(0.), new Timestamp(0L), new Timestamp(0L));
        AttributeValue<Double> floor = AttributeValueFactory.newAttributeValue(fullName, null, Value.getInstance(10.), new Timestamp(10L), new Timestamp(10L));

        double result = Interpolation.LINEAR.interpolate(ceil, floor, new Timestamp(0L)).getValue().get();

        assertEquals(0., result);
    }

    @Test
    public void test_Linear_CornerRight() {
        AttributeValue<Double> ceil = AttributeValueFactory.newAttributeValue(fullName, null, Value.getInstance(0.), new Timestamp(0L), new Timestamp(0L));
        AttributeValue<Double> floor = AttributeValueFactory.newAttributeValue(fullName, null, Value.getInstance(10.), new Timestamp(10L), new Timestamp(10L));

        double result = Interpolation.LINEAR.interpolate(ceil, floor, new Timestamp(10L)).getValue().get();

        assertEquals(10., result);
    }

}
