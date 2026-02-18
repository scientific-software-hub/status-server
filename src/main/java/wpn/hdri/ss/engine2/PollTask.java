package wpn.hdri.ss.engine2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.client.ClientException;
import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.writer.RecordWriter;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 10.11.2015
 */
public class PollTask extends AbsTask implements Runnable {

    public PollTask(Attribute<?> attr, RecordWriter writer, boolean append) {
        super(attr, writer, append);
    }

    @Override
    public void run() {
        try {
            SingleRecord<?> result = attr.devClient.read(attr);
            writer.write(result, append);
        } catch (ClientException e) {
            logger.error(e.getMessage());
        }
    }
}
