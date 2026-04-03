package wpn.hdri.ss.engine2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.event.EventSink;
import wpn.hdri.ss.event.ReadSuccess;
import wpn.hdri.ss.event.TechnicalEvent;

import java.time.Instant;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 10.11.2015
 */
public class EventTask extends AbsTask {
    private static final Logger logger = LoggerFactory.getLogger(EventTask.class);

    private Runnable resubscribeCallback;

    public EventTask(Attribute attr, EventSink<SingleRecord<?>> sink, EventSink<TechnicalEvent> technicalSink) {
        super(attr, sink, technicalSink);
    }

    /**
     * Called by Engine so that when onError() fires, the engine can queue a re-subscription attempt.
     */
    public void setResubscribeCallback(Runnable resubscribeCallback) {
        this.resubscribeCallback = resubscribeCallback;
    }

    public void onEvent(SingleRecord<?> record) {
        sink.onEvent(record);
        technicalSink.onEvent(new ReadSuccess(attr.id, Instant.now()));
    }

    public void onError(Exception e) {
        logger.warn("{}/{}: {}", attr.devClient, attr.name, e.getMessage());
        TechnicalEvent tech = classifyException(e);
        sink.onEvent(failedRecord(tech));
        technicalSink.onEvent(tech);
        if (resubscribeCallback != null) {
            resubscribeCallback.run();
        }
    }
}
