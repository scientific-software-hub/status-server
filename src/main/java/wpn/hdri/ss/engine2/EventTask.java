package wpn.hdri.ss.engine2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.client.EventCallback;
import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.writer.RecordWriter;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 10.11.2015
 */
public class EventTask extends AbsTask {
    private static final Logger logger = LoggerFactory.getLogger(EventTask.class);

    public EventTask(Attribute attr, RecordWriter writer, boolean append) {
        super(attr, writer, append);
    }

    public void onEvent(SingleRecord<?> record) {
        writer.write(record, append);
    }
}
