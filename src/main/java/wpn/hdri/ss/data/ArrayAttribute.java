package wpn.hdri.ss.data;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Map;

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

        if (!Arrays.equals((Object[])lastValue.getValue().get(), (Object[])value.get())) {
            storage.addValue(attributeValue);
        }
    }
}
