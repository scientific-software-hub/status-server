package wpn.hdri.ss.storage;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 23.04.13
 */
//TODO replace with annotations
public interface TypeFactory<T> {
    T createType(String dataName, Iterable<String> header, Iterable<String> values);
}
