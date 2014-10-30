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
        assertEquals("1.0E-6", lowPres);
    }

    @Test
    public void testDoubleFormatter_HighPrecision(){
        ValueFormatter<Double> fmt = ValueFormatters.DOUBLE_FORMATTER;
        String highPres = fmt.format(1.0E-9);

        assertEquals("1.0E-9", highPres);
    }

    @Test
    public void testDoubleFormatter_OrdinaryValue(){
        ValueFormatter<Double> fmt = ValueFormatters.DOUBLE_FORMATTER;
        String highPres = fmt.format(123.00003D);

        assertEquals("123.00003", highPres);
    }

    @Test
    public void testDoubleFormatter_OrdinaryValue_1(){
        ValueFormatter<Double> fmt = ValueFormatters.DOUBLE_FORMATTER;
        String highPres = fmt.format(123.D);

        assertEquals("123.0", highPres);
    }

    @Test
    public void testDoubleFormatter_OrdinaryValue_2(){
        ValueFormatter<Double> fmt = ValueFormatters.DOUBLE_FORMATTER;
        String highPres = fmt.format(0.009D);

        //ouch
        assertEquals("0.008999999999999999", highPres);
    }

    @Test
    public void testDoubleFormatter_OrdinaryValue_3(){
        ValueFormatter<Double> fmt = ValueFormatters.DOUBLE_FORMATTER;
        String highPres = fmt.format(0.0098D);

        assertEquals("0.0098", highPres);
    }

    @Test
    public void testDoubleFormatter_OrdinaryValue_4(){
        ValueFormatter<Double> fmt = ValueFormatters.DOUBLE_FORMATTER;
        String highPres = fmt.format(0.000123001D);

        assertEquals("1.23001E-4", highPres);
    }

    @Test
    public void testDoubleFormatter_OrdinaryNegativeValue(){
        ValueFormatter<Double> fmt = ValueFormatters.DOUBLE_FORMATTER;
        String highPres = fmt.format(-123.00003D);

        assertEquals("-123.00003", highPres);
    }

    @Test
    public void testDoubleFormatter_OrdinaryNegativeValue_1(){
        ValueFormatter<Double> fmt = ValueFormatters.DOUBLE_FORMATTER;
        String highPres = fmt.format(-2.0D);

        assertEquals("-2.0", highPres);
    }

    @Test
     public void testDoubleFormatter_OrdinaryNegativeValue_2(){
        ValueFormatter<Double> fmt = ValueFormatters.DOUBLE_FORMATTER;
        String highPres = fmt.format(-0.0098D);

        assertEquals("-0.0098", highPres);
    }

    @Test
    public void testDoubleFormatter_OrdinaryNegativeValue_3(){
        ValueFormatter<Double> fmt = ValueFormatters.DOUBLE_FORMATTER;
        String highPres = fmt.format(-0.0008D);

        //ouch
        assertEquals("-8.000000000000001E-4", highPres);
    }

    @Test
    public void testDoubleFormatter_PositiveInfinity(){
        ValueFormatter<Double> fmt = ValueFormatters.DOUBLE_FORMATTER;
        String highPres = fmt.format(Double.POSITIVE_INFINITY);

        assertEquals("Infinity", highPres);
    }

    @Test
    public void testDoubleFormatter_NegativeInfinity(){
        ValueFormatter<Double> fmt = ValueFormatters.DOUBLE_FORMATTER;
        String highPres = fmt.format(Double.NEGATIVE_INFINITY);

        assertEquals("-Infinity", highPres);
    }

    @Test
    public void testDoubleFormatter_NaN(){
        ValueFormatter<Double> fmt = ValueFormatters.DOUBLE_FORMATTER;
        String highPres = fmt.format(Double.NaN);

        assertEquals("NaN", highPres);
    }

    @Test
    public void testDoubleFormatter_MaxValue(){
        ValueFormatter<Double> fmt = ValueFormatters.DOUBLE_FORMATTER;
        String highPres = fmt.format(Double.MAX_VALUE);

        assertEquals("1.797693134862316E308", highPres);
    }

    @Test
    public void testDoubleFormatter_MinValue(){
        ValueFormatter<Double> fmt = ValueFormatters.DOUBLE_FORMATTER;
        String highPres = fmt.format(Double.MIN_VALUE);

        assertEquals("5.0E-324", highPres);
    }

}
