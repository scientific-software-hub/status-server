package wpn.hdri.ss.client2;

import wpn.hdri.ss.client.ClientException;
import wpn.hdri.ss.client.EventCallback;
import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.engine2.EventTask;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 10.11.2015
 */
public interface ClientAdaptor {
    public <T> SingleRecord<T> read(Attribute<T> attr) throws ClientException;

    /**
     * Fails silently
     *
     * @param eventTask
     */
    public void subscribe(EventTask eventTask);

    /**
     * Fails silently
     *
     * @param attr
     */
    public void unsubscribe(Attribute<?> attr);
}
