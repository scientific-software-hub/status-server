package wpn.hdri.ss.client;

import org.tango.client.ez.proxy.EventData;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 29.11.12
 */
public interface EventCallback<T> {
    void onEvent(EventData<T> data);

    void onError(Exception ex);
}
