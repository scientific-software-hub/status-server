package hzg.wpn.util.conveter;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 25.04.13
 */
public interface TypeConverter<S,T> {
    T convert(S src);
}
