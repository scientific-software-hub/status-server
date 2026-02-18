package wpn.hdri.ss.engine2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.writer.RecordWriter;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 10.11.2015
 */
public class AbsTask {
    protected final static Logger logger = LoggerFactory.getLogger(AbsTask.class);
    protected final Attribute attr;
    protected final RecordWriter writer;
    protected final boolean append;

    public AbsTask(Attribute attr, RecordWriter writer, boolean append) {
        this.attr = attr;
        this.writer = writer;
        this.append = append;
    }

    public Attribute getAttribute() {
        return attr;
    }
}
