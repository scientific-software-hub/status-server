package wpn.hdri.ss.data;

import org.apache.commons.lang3.ArrayUtils;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 30.11.12
 */
public class ArrayAttribute extends Attribute<Object> {
    public ArrayAttribute(String devName, String attrName, String alias) {
        super(devName, attrName, alias, Interpolation.LAST);
    }

    @Override
    public void addValue(Timestamp readTimestamp, Value<? super Object> value, Timestamp writeTimestamp) {
        AttributeValue<Object> attributeValue = AttributeHelper.newAttributeValue(getFullName(), getAlias(), value, readTimestamp, writeTimestamp);

        AttributeValue<Object> lastValue = storage.getLastValue();
        if (lastValue == null) {
            storage.addValue(attributeValue);
        } else if (!ArrayUtils.isEquals(lastValue.getValue().get(), value.get())) {
            storage.addValue(attributeValue);
        }
    }
}
