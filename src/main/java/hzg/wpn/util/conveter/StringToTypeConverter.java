package hzg.wpn.util.conveter;

/**
 * Converts Strings to java build-in types
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 25.04.13
 */
public abstract class StringToTypeConverter<T> implements TypeConverter<String,T> {
    //TODO avoid auto boxing
    public static StringToTypeConverter<Double> TO_DOUBLE = new StringToTypeConverter<Double>() {
        @Override
        public Double convert(String s) {
            return Double.parseDouble(s);
        }
    };

    public static StringToTypeConverter<Float> TO_FLOAT = new StringToTypeConverter<Float>() {
        @Override
        public Float convert(String s) {
            return Float.parseFloat(s);
        }
    };

    public static StringToTypeConverter<Byte> TO_BYTE = new StringToTypeConverter<Byte>() {
        @Override
        public Byte convert(String s) {
            return Byte.parseByte(s);
        }
    };

    public static StringToTypeConverter<Short> TO_SHORT = new StringToTypeConverter<Short>() {
        @Override
        public Short convert(String s) {
            return Short.parseShort(s);
        }
    };

    public static StringToTypeConverter<Integer> TO_INT = new StringToTypeConverter<Integer>() {
        @Override
        public Integer convert(String s) {
            return Integer.parseInt(s);
        }
    };

    public static StringToTypeConverter<Long> TO_LONG = new StringToTypeConverter<Long>() {
        @Override
        public Long convert(String s) {
            return Long.parseLong(s);
        }
    };

    public static StringToTypeConverter<Character> TO_CHAR = new StringToTypeConverter<Character>() {
        @Override
        public Character convert(String s) {
            return s.charAt(0);
        }
    };

    public static StringToTypeConverter<Boolean> TO_BOOL = new StringToTypeConverter<Boolean>() {
        @Override
        public Boolean convert(String s) {
            return Boolean.parseBoolean(s);
        }
    };

    public static StringToTypeConverter<String> TO_STRING = new StringToTypeConverter<String>() {
        @Override
        public String convert(String s) {
            return s;
        }
    };

    public abstract T convert(String s);
}
