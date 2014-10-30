package wpn.hdri.ss.data.attribute;

import com.google.common.collect.Iterables;
import hzg.wpn.util.conveter.TypeConverter;
import hzg.wpn.util.conveter.TypeConverters;
import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.data.Value;
import wpn.hdri.ss.storage.TypeFactory;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 23.04.13
 */
public class AttributeValueFactory<T> implements TypeFactory<AttributeValue<T>> {
    public static <T> AttributeValue<T> newAttributeValue(String fullName, String alias, Value<? super T> value, Timestamp readTimestamp, Timestamp writeTimestamp) {
        return new AttributeValue<T>(fullName, alias, value, readTimestamp, writeTimestamp);
    }

    /**
     * @param dataName
     * @param header
     * @param values
     * @return new AttributeValue instance or null
     */
    @Override
    public AttributeValue<T> createType(String dataName, Iterable<String> header, Iterable<String> values) {
        try {
            String alias = Iterables.get(values, 1);
            Class<T> clazz = (Class<T>) this.getClass().getClassLoader().loadClass(Iterables.get(values, 2));

            String value = Iterables.get(values, 3);

            TypeConverter<String, T> converter = TypeConverters.lookupStringToTypeConverter(clazz);
            if (converter == null) {
                //TODO log
                return null;
            }

            Value<T> val = Value.getInstance(converter.convert(value));

            Timestamp read = Timestamp.fromString(Iterables.get(values, 4));
            Timestamp write = Timestamp.fromString(Iterables.get(values, 5));

            return newAttributeValue(dataName, alias, val, read, write);
        } catch (ClassNotFoundException e) {
            //TODO log
            return null;
        }
    }
}
