package wpn.hdri.ss.engine;

import org.apache.log4j.Logger;
import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.client.EventCallback;
import wpn.hdri.ss.client.EventData;
import wpn.hdri.ss.data.Attribute;
import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.data.Value;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 17.06.13
 */
public class EventReadAttributeTask implements EventCallback<Object> {
    private final Attribute<?> attribute;
    private final Client devClient;

    private final boolean append;

    private final Logger logger;

    public EventReadAttributeTask(Attribute<?> attribute, Client devClient, boolean append, Logger logger) {
        this.attribute = attribute;
        this.devClient = devClient;
        this.append = append;
        this.logger = logger;
    }

    /**
     * Fired from successful event read attempt.
     *
     * @param eventData new value
     */
    public final void onEvent(EventData<Object> eventData) {
        Timestamp timestamp = Timestamp.now();
        attribute.addValue(timestamp, Value.getInstance(eventData.getData()), new Timestamp(eventData.getTimestamp()), append);
    }

    /**
     * Fired from failed event read attempt.
     *
     * @param ex cause
     */
    public final void onError(Throwable ex) {
        logger.warn("Can not read from " + attribute.getFullName(), ex);
//        Timestamp timestamp = Timestamp.now();
//
//        attribute.addValue(timestamp, Value.NULL, timestamp);
    }

    public Attribute<?> getAttribute() {
        return attribute;
    }

    public Client getDevClient() {
        return devClient;
    }
}
