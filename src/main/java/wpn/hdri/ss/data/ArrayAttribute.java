package wpn.hdri.ss.data;

import java.lang.reflect.Array;
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
        Map.Entry<Timestamp, AttributeValue<Object>> lastEntry = values.lastEntry();
        if (lastEntry == null) {
            values.putIfAbsent(readTimestamp, attributeValue);
            latestValue.set(attributeValue);
            return;
        }
        AttributeValue<Object> lastValue = lastEntry.getValue();
        if (!isArrayEquals(lastValue.getValue().get(), value.get())) {
            values.putIfAbsent(readTimestamp, attributeValue);
            latestValue.set(attributeValue);
        }
    }

    private boolean isArrayEquals(Object o, Object o1) {
        int oSize = Array.getLength(o);
        int o1Size = Array.getLength(o1);
        if(oSize != o1Size)
            return false;

        for(int i = 0, size = oSize; i < size; i++){
            Object oItem = Array.get(o,i);
            Object o1Item = Array.get(o1,i);
            if(!oItem.equals(o1Item)){
                return false;
            }
        }

        return true;
    }


}
