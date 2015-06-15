package wpn.hdri.ss.data.attribute;

import wpn.hdri.ss.data.Interpolation;

import java.math.BigDecimal;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 14.06.13
 */
public class AttributeFactory {
    public Attribute<?> createAttribute(String attrName, String attrAlias, String devName, Interpolation interpolation, BigDecimal precision, Class<?> type, boolean isArray) {
        if (isArray) {
            return new ArrayAttribute(devName, attrName, attrAlias);
        } else
            //consider char as numeric type
            if (Number.class.isAssignableFrom(type) || (type.isPrimitive() && type != boolean.class)) {
                return new NumericAttribute<Number>(devName, attrName, attrAlias, interpolation, precision);
            } else {
                return new NonNumericAttribute<Object>(devName, attrName, attrAlias, interpolation);
            }
    }
}
