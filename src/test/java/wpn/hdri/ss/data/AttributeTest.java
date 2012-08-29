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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static junit.framework.Assert.*;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 27.04.12
 */
public class AttributeTest {
    private final String deviceName = "TestDevice";
    private final String name = "Test";
    private long timestamp;

    @Before
    public void before() {
        timestamp = System.currentTimeMillis();
    }

    @Test
    public void testGetValue() throws Exception {
        Attribute<Long> attr = new NumericAttribute<Long>(deviceName, name, Interpolation.LAST);

        attr.addValue(timestamp, Value.<Long>getInstance(10000L), timestamp);

        attr.addValue(timestamp + 10, Value.<Long>getInstance(10001L), timestamp);

        long result = attr.getAttributeValue(timestamp + 5).getValue().get();

        assertEquals(10000L, result);
    }

    @Test
    public void testGetValue_Last() throws Exception {
        Attribute<Long> attr = new NumericAttribute<Long>(deviceName, name, Interpolation.LAST);

        attr.addValue(timestamp, Value.<Long>getInstance(10000L), timestamp);

        attr.addValue(timestamp + 10, Value.<Long>getInstance(10001L), timestamp);

        long result = attr.getAttributeValue().getValue().get();

        assertEquals(10001L, result);
    }

    @Test
    public void testGetValues() throws Exception {
        Attribute<Long> attr = new NumericAttribute<Long>(deviceName, name, Interpolation.LAST);

        attr.addValue(timestamp, Value.<Long>getInstance(10000L), timestamp);

        attr.addValue(timestamp + 10, Value.<Long>getInstance(10001L), timestamp);

        int attributeSize = Iterables.toArray(attr.getAttributeValues(), AttributeValue.class).length;

        assertTrue(attributeSize == 2);
    }

    @Test
    public void testGetValues_Bounded() throws Exception {
        Attribute<Long> attr = new NumericAttribute<Long>(deviceName, name, Interpolation.LAST);

        attr.addValue(timestamp, Value.<Long>getInstance(10000L), timestamp);

        attr.addValue(timestamp + 10, Value.<Long>getInstance(10001L), timestamp);

        int attributeSize = Iterables.toArray(attr.getAttributeValues(timestamp + 5), AttributeValue.class).length;

        assertTrue(attributeSize == 1);
    }

    @Test
    public void testGetValue_LastNearest() throws Exception {
        Attribute<Long> attr = new NumericAttribute<Long>(deviceName, name, Interpolation.LAST);

        attr.addValue(timestamp, Value.<Long>getInstance(10000L), timestamp);

        attr.addValue(timestamp + 10, Value.<Long>getInstance(10001L), timestamp);

        long result = attr.getAttributeValue(timestamp + 15).getValue().get();

        assertEquals(10001L, result);
    }

    @Test
    public void testGetValue_Linear() throws Exception {
        Attribute<Double> attr = new NumericAttribute<Double>(deviceName, name, Interpolation.LINEAR);

        attr.addValue(timestamp, Value.<Double>getInstance(12.), timestamp);

        attr.addValue(timestamp + 10, Value.<Double>getInstance(24.), timestamp + 10);

        double result = attr.getAttributeValue(timestamp + 5).getValue().get();

        assertEquals(18., result);
    }

    @Test
    public void testGetValue_Linear_missedRightValue() {
        Attribute<Long> attribute = new NumericAttribute<Long>(deviceName, name, Interpolation.LINEAR);

        attribute.addValue(timestamp, Value.getInstance(1000L), timestamp);
        attribute.addValue(timestamp + 10, Value.getInstance(1000L), timestamp);

        long result = attribute.getAttributeValue(timestamp + 20).getValue().get();

        assertEquals(1000L, result);
    }

    @Test
    public void testPrecision_SameValue() {
        Attribute<Double> attribute = new NumericAttribute<Double>(deviceName, name, Interpolation.LINEAR, BigDecimal.ONE);

        attribute.addValue(timestamp, Value.getInstance(20.3), timestamp);
        attribute.addValue(timestamp + 10, Value.getInstance(20.1), timestamp);
        attribute.addValue(timestamp + 20, Value.getInstance(20.5), timestamp);

        double result = attribute.getAttributeValue().getValue().get();

        assertEquals(20.3, result);
    }

    @Test
    public void testPrecision_SameValue_morePrecision() {
        Attribute<Double> attribute = new NumericAttribute<Double>(deviceName, name, Interpolation.LINEAR, new BigDecimal("0.001"));

        attribute.addValue(timestamp, Value.getInstance(0.2441406), timestamp);
        attribute.addValue(timestamp + 10, Value.getInstance(0.2453613), timestamp);
        attribute.addValue(timestamp + 15, Value.getInstance(0.2454613), timestamp);

        double result = attribute.getAttributeValue().getValue().get();

        assertEquals(0.2453613, result);
        int attributeSize = Iterables.toArray(attribute.getAttributeValues(), AttributeValue.class).length;
        assertTrue(attributeSize == 2);
    }

    @Test
    public void testSameValue_Numeric() {
        Attribute<Double> attribute = new NumericAttribute<Double>(deviceName, name, Interpolation.LINEAR, BigDecimal.ONE);

        attribute.addValue(timestamp, Value.getInstance(20.3), timestamp);
        attribute.addValue(timestamp + 10, Value.getInstance(20.3), timestamp);

        double result = attribute.getAttributeValue().getValue().get();

        assertEquals(20.3, result);
        int attributeSize = Iterables.toArray(attribute.getAttributeValues(), AttributeValue.class).length;
        assertTrue(attributeSize == 1);
    }


    @Test
    public void testPrecision_SameValueDifferentTime() {
        Attribute<String> attribute = new NonNumericAttribute<String>(deviceName, name, Interpolation.LAST);

        attribute.addValue(timestamp, Value.getInstance("Rabbit"), timestamp);
        attribute.addValue(timestamp + 10, Value.getInstance("Rabbit"), timestamp);

        String result = attribute.getAttributeValue().getValue().get();

        assertEquals("Rabbit", result);
        assertEquals(timestamp, attribute.getAttributeValue().getWriteTimestamp().getValue());
        int attributeSize = Iterables.toArray(attribute.getAttributeValues(), AttributeValue.class).length;
        assertTrue(attributeSize == 1);
    }

    @Test
    public void testPrecision_DifferentValue() {
        Attribute<Double> attribute = new NumericAttribute<Double>(deviceName, name, Interpolation.LINEAR, BigDecimal.ONE);

        attribute.addValue(timestamp, Value.getInstance(20.3), timestamp);
        attribute.addValue(timestamp + 10, Value.getInstance(20.7), timestamp);
        attribute.addValue(timestamp + 20, Value.getInstance(21.4), timestamp);

        double result = attribute.getAttributeValue().getValue().get();

        assertEquals(21.4, result);
    }

    @Test
    public void testAddNull_Numeric() {
        Attribute<Double> attribute = new NumericAttribute<Double>(deviceName, name, Interpolation.LAST);

        attribute.addValue(timestamp, Value.getInstance(12.), timestamp);
        attribute.addValue(timestamp + 10, Value.NULL, timestamp);

        Double result = attribute.getAttributeValue().getValue().get();
        assertNull(result);
    }

    @Test
    public void testAddNull_NonNumeric() {
        Attribute<Boolean> attribute = new NonNumericAttribute<Boolean>(deviceName, name, Interpolation.LAST);

        attribute.addValue(timestamp, Value.getInstance(false), timestamp);
        attribute.addValue(timestamp + 10, Value.NULL, timestamp);

        Boolean result = attribute.getAttributeValue().getValue().get();
        assertNull(result);
    }

    @Test
    public void testGetTailValues() {
        Attribute<String> attribute = new NonNumericAttribute<String>(deviceName, name, Interpolation.LAST);

        attribute.addValue(timestamp, Value.getInstance("Hello"), timestamp);
        attribute.addValue(timestamp + 10, Value.getInstance("World"), timestamp);
        attribute.addValue(timestamp + 20, Value.getInstance("!!!"), timestamp);

        Iterable<AttributeValue<String>> result = attribute.getAttributeValues(timestamp + 5);

        String[] resultArray = Iterables.toArray(Iterables.transform(result, new Function<AttributeValue<String>, String>() {
            @Override
            public String apply(AttributeValue<String> input) {
                return input.getValue().get();
            }
        }), String.class);
        String value1 = resultArray[0];
        assertEquals("World", value1);
        String value2 = resultArray[1];
        assertEquals("!!!", value2);
    }

    @Test
    public void testSetNullValue() {
        Attribute<String> attribute = new NonNumericAttribute<String>(deviceName, name, Interpolation.LAST);

        attribute.addValue(timestamp, Value.getInstance(null), timestamp);

        String result = attribute.getAttributeValue().getValue().get();

        assertNull(result);
    }
}
