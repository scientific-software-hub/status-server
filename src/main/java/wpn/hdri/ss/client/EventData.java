package wpn.hdri.ss.client;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 29.11.12
 */
public class EventData<T> {
    private final T data;
    private final long timestamp;

    public EventData(T data, long timestamp) {
        this.data = data;
        this.timestamp = timestamp;
    }


    public T getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
