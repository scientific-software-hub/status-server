package wpn.hdri.ss.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tango.client.ez.proxy.EventData;
import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.client.EventCallback;
import wpn.hdri.ss.data.Method;
import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.data.Value;
import wpn.hdri.ss.data.attribute.Attribute;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 17.06.13
 */
public class EventReadAttributeTask implements EventCallback<Object> {
    private static final Logger logger = LoggerFactory.getLogger(EventReadAttributeTask.class);

    private final Attribute<?> attribute;

    private final Client devClient;

    private final boolean append;
    private Method.EventType eventType;

    public EventReadAttributeTask(Attribute<?> attribute, Method.EventType eventType, Client devClient, boolean append) {
        this.attribute = attribute;
        this.devClient = devClient;
        this.append = append;
        this.eventType = eventType;
    }

    /**
     * Fired from successful event read attempt.
     *
     * @param eventData new value
     */
    public final void onEvent(EventData<Object> eventData) {
        if (append)
            attribute.addValue(Timestamp.now(), Value.getInstance(eventData.getValue()), new Timestamp(eventData.getTime()));
        else
            attribute.replaceValue(Timestamp.now(), Value.getInstance(eventData.getValue()), new Timestamp(eventData.getTime()));
    }

    /**
     * Fired from failed event read attempt.
     *
     * @param ex cause
     */
    public final void onError(Exception ex) {
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

    public Method.EventType getEventType() {
        return eventType;
    }
}
