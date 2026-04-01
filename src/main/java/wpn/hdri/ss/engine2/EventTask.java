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

    public EventTask(Attribute attr, EventSink<SingleRecord<?>> sink, EventSink<TechnicalEvent> technicalSink) {
        super(attr, sink, technicalSink);
    }

    public void onEvent(SingleRecord<?> record) {
        sink.onEvent(record);
        technicalSink.onEvent(new ReadSuccess(attr.id, Instant.now()));
    }

    public void onError(Exception e) {
        logger.warn("{}/{}: {}", attr.devClient, attr.name, e.getMessage());
        sink.onEvent(new SingleRecord<>(attr, System.currentTimeMillis(), 0, null));
        technicalSink.onEvent(classifyException(e));
    }
}
