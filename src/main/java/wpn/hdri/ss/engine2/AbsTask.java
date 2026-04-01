package wpn.hdri.ss.engine2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.event.EventSink;
import wpn.hdri.ss.event.ReadFailure;
import wpn.hdri.ss.event.TechnicalEvent;
import wpn.hdri.ss.event.Timeout;

import java.time.Instant;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 10.11.2015
 */
public class AbsTask {
    protected final static Logger logger = LoggerFactory.getLogger(AbsTask.class);
    protected final Attribute attr;
    protected final EventSink<SingleRecord<?>> sink;
    protected final EventSink<TechnicalEvent> technicalSink;

    public AbsTask(Attribute attr, EventSink<SingleRecord<?>> sink, EventSink<TechnicalEvent> technicalSink) {
        this.attr = attr;
        this.sink = sink;
        this.technicalSink = technicalSink;
    }

    public Attribute getAttribute() {
        return attr;
    }

    protected TechnicalEvent classifyException(Exception e) {
        Instant now = Instant.now();
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        String msg = cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";
        if (msg.contains("timeout") || msg.contains("timed out")) {
            return new Timeout(attr.id, now);
        }
        return new ReadFailure(attr.id, now, e.getMessage());
    }
}
