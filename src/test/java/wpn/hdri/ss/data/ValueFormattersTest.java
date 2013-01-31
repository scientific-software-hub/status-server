package wpn.hdri.ss.data;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 30.01.13
 */
public class ValueFormattersTest {
    @Test
    public void testDoubleFormatter_LowPrecision(){
        ValueFormatter<Double> fmt = ValueFormatters.DOUBLE_FORMATTER;
        String lowPres = fmt.format(1.0E-6);

        //due to doubles nature we have to tolerate this last zero
        assertEquals("0.000001", lowPres);
    }

    @Test
    public void testDoubleFormatter_HighPrecision(){
        ValueFormatter<Double> fmt = ValueFormatters.DOUBLE_FORMATTER;
        String highPres = fmt.format(1.0E-9);

        assertEquals("0.000000001", highPres);
    }

    @Test
    public void testDoubleFormatter_OrdinaryValue(){
        ValueFormatter<Double> fmt = ValueFormatters.DOUBLE_FORMATTER;
        String highPres = fmt.format(123.00003D);

        assertEquals("123.00003", highPres);
    }

    @Test
    public void testDoubleFormatter_OrdinaryNegativeValue(){
        ValueFormatter<Double> fmt = ValueFormatters.DOUBLE_FORMATTER;
        String highPres = fmt.format(-123.00003D);

        assertEquals("-123.00003", highPres);
    }

    @Test
    public void testDoubleFormatter_MaxValue(){
        ValueFormatter<Double> fmt = ValueFormatters.DOUBLE_FORMATTER;
        String highPres = fmt.format(Double.MAX_VALUE);

        assertEquals("9223372036854775807.0", highPres);
    }

    @Test
    public void testDoubleFormatter_MinValue(){
        ValueFormatter<Double> fmt = ValueFormatters.DOUBLE_FORMATTER;
        String highPres = fmt.format(Double.MIN_VALUE);

        //exceeds maximum precision
        assertEquals("1.0E-128", highPres);
    }

}
