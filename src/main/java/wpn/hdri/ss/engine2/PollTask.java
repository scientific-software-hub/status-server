package wpn.hdri.ss.engine2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.client.ClientException;
import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.event.EventSink;
import wpn.hdri.ss.event.ReadFailure;
import wpn.hdri.ss.event.ReadSuccess;
import wpn.hdri.ss.event.TechnicalEvent;

import java.time.Instant;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 10.11.2015
 */
public class PollTask extends AbsTask implements Runnable {

    public PollTask(Attribute<?> attr, EventSink<SingleRecord<?>> sink, EventSink<TechnicalEvent> technicalSink) {
        super(attr, sink, technicalSink);
    }

    @Override
    public void run() {
        try {
            SingleRecord<?> result = attr.devClient.read(attr);
            sink.onEvent(result);
            technicalSink.onEvent(new ReadSuccess(attr.id, Instant.now()));
        } catch (ClientException e) {
            logger.warn("{}/{}: {}", attr.devClient, attr.name, e.getMessage());
            TechnicalEvent tech = classifyException(e);
            sink.onEvent(failedRecord(tech));
            technicalSink.onEvent(tech);
        } catch (Exception e) {
            // Never let an unchecked exception escape run(): ScheduledExecutorService silently
            // cancels all future executions of a periodic task on an uncaught throwable, with no
            // log line, which would kill polling for this attribute permanently.
            logger.error("{}/{}: unexpected error in poll task, task survives", attr.devClient, attr.name, e);
            TechnicalEvent tech = new ReadFailure(attr.id, Instant.now(), e.toString());
            sink.onEvent(failedRecord(tech));
            technicalSink.onEvent(tech);
        }
    }
}
