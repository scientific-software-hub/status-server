package wpn.hdri.ss.engine2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tango.client.ez.proxy.EventData;
import wpn.hdri.ss.client.EventCallback;
import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.data2.SingleRecord;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 10.11.2015
 */
public class EventTask extends AbsTask implements EventCallback {
    private static final Logger logger = LoggerFactory.getLogger(EventTask.class);



    public EventTask(Attribute attr, DataStorage storage, boolean append) {
        super(attr, storage, append);
    }

    @Override
    public void onEvent(EventData data) {
        SingleRecord record = new SingleRecord(attr.ndx, System.currentTimeMillis(), data.getTime(), data.getValue());
        if(append)
            storage.appendRecord(record);
         else
            storage.writeRecord(record);
    }

    @Override
    public void onError(Exception ex) {
        logger.error(ex.toString());
    }
}
