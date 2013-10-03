package wpn.hdri.ss.data.attribute;

import org.junit.Test;
import wpn.hdri.ss.data.Interpolation;

import java.math.BigDecimal;

import static junit.framework.Assert.assertTrue;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 14.06.13
 */
public class AttributeFactoryTest {
    private AttributeFactory instance = new AttributeFactory();

    @Test
    public void testCreateAttribute_primitive() throws Exception {
        Attribute result = instance.createAttribute("test-attr", null, "test", Interpolation.LAST, BigDecimal.ZERO, float.class, false);

        assertTrue(NumericAttribute.class == result.getClass());
    }

    @Test
    public void testCreateAttribute_Numeric() throws Exception {
        Attribute result = instance.createAttribute("test-attr", null, "test", Interpolation.LAST, BigDecimal.ZERO, Float.class, false);

        assertTrue(NumericAttribute.class == result.getClass());
    }

    @Test
    public void testCreateAttribute_NonNumeric() throws Exception {
        Attribute result = instance.createAttribute("test-attr", null, "test", Interpolation.LAST, BigDecimal.ZERO, String.class, false);

        assertTrue(NonNumericAttribute.class == result.getClass());
    }
}
