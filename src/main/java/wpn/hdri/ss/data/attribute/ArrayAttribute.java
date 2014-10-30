package wpn.hdri.ss.data.attribute;

import org.apache.commons.lang3.ArrayUtils;
import wpn.hdri.ss.data.Interpolation;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 30.11.12
 */
public class ArrayAttribute extends Attribute<Object> {
    public ArrayAttribute(String devName, String attrName, String alias) {
        super(devName, attrName, alias, Interpolation.LAST);
    }

    /**
     * Adds a new value to this attribute if the new array is different from the last one.
     *
     * @param value value
     */
    @Override
    protected boolean addValueInternal(AttributeValue value) {
        return !ArrayUtils.isEquals(getAttributeValue().getValue().get(), value.getValue().get());
    }
}
