package hzg.wpn.util.conveter;

import com.google.common.collect.Maps;
import sun.plugin.com.TypeConverter;

import java.util.Map;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 25.04.13
 */
public class TypeConverters {
    private TypeConverters(){}

    private static final Map<Class<?>,StringToTypeConverter<?>> STRING_TO_TYPE_CONVERTERS = Maps.newIdentityHashMap();

    static {
        STRING_TO_TYPE_CONVERTERS.put(Double.class,StringToTypeConverter.TO_DOUBLE);
        STRING_TO_TYPE_CONVERTERS.put(double.class,StringToTypeConverter.TO_DOUBLE);
        STRING_TO_TYPE_CONVERTERS.put(Float.class,StringToTypeConverter.TO_FLOAT);
        STRING_TO_TYPE_CONVERTERS.put(float.class,StringToTypeConverter.TO_FLOAT);
        STRING_TO_TYPE_CONVERTERS.put(Byte.class,StringToTypeConverter.TO_BYTE);
        STRING_TO_TYPE_CONVERTERS.put(byte.class,StringToTypeConverter.TO_BYTE);
        STRING_TO_TYPE_CONVERTERS.put(Short.class,StringToTypeConverter.TO_SHORT);
        STRING_TO_TYPE_CONVERTERS.put(short.class,StringToTypeConverter.TO_SHORT);
        STRING_TO_TYPE_CONVERTERS.put(Integer.class,StringToTypeConverter.TO_INT);
        STRING_TO_TYPE_CONVERTERS.put(int.class,StringToTypeConverter.TO_INT);
        STRING_TO_TYPE_CONVERTERS.put(Long.class,StringToTypeConverter.TO_LONG);
        STRING_TO_TYPE_CONVERTERS.put(long.class,StringToTypeConverter.TO_LONG);
        STRING_TO_TYPE_CONVERTERS.put(Character.class,StringToTypeConverter.TO_CHAR);
        STRING_TO_TYPE_CONVERTERS.put(char.class,StringToTypeConverter.TO_CHAR);
        STRING_TO_TYPE_CONVERTERS.put(Boolean.class,StringToTypeConverter.TO_BOOL);
        STRING_TO_TYPE_CONVERTERS.put(boolean.class,StringToTypeConverter.TO_BOOL);
        STRING_TO_TYPE_CONVERTERS.put(String.class,StringToTypeConverter.TO_STRING);
    }

    public static <T> hzg.wpn.util.conveter.TypeConverter<String,T> lookupStringToTypeConverter(Class<T> clazz){
        return (hzg.wpn.util.conveter.TypeConverter<String, T>) STRING_TO_TYPE_CONVERTERS.get(clazz);
    }
}
