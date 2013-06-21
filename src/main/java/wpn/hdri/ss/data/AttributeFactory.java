package wpn.hdri.ss.data;

import java.math.BigDecimal;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 14.06.13
 */
public class AttributeFactory {
    public Attribute<?> createAttribute(String attrName, String attrAlias, String devName, Interpolation interpolation, BigDecimal precision, Class<?> type, boolean isArray, AttributeValuesStorageFactory storageFactory) {
        if (isArray) {
            return new ArrayAttribute(devName, attrName, attrAlias, storageFactory);
        } else
            //consider char as numeric type
            if (Number.class.isAssignableFrom(type) || (type.isPrimitive() && type != boolean.class)) {
                return new NumericAttribute<Number>(devName, attrName, attrAlias, interpolation, precision, storageFactory);
            } else {
                return new NonNumericAttribute<Object>(devName, attrName, attrAlias, interpolation, storageFactory);
            }
    }
}
