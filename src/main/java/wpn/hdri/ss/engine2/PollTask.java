package wpn.hdri.ss.engine2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.client.ClientException;
import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.data2.SingleRecord;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 10.11.2015
 */
public class PollTask extends AbsTask implements Runnable {



    public PollTask(Attribute<?> attr, DataStorage storage, boolean append) {
        super(attr, storage, append);
    }

    @Override
    public void run() {
        try {
            SingleRecord<?> result = attr.devClient.read(attr);

            if(append)
                storage.appendRecord(result);
            else
                storage.writeRecord(result);
        } catch (ClientException e) {
            logger.error(e.getMessage());
        }
    }
}
