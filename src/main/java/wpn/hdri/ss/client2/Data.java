package wpn.hdri.ss.client2;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 10.11.2015
 */
public class Data<T> {
    public final long timestamp;
    public final T value;

    public Data(long timestamp, T value) {
        this.timestamp = timestamp;
        this.value = value;
    }
}
