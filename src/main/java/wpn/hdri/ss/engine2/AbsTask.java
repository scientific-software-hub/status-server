package wpn.hdri.ss.engine2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.data2.Attribute;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 10.11.2015
 */
public class AbsTask {
    protected final static Logger logger = LoggerFactory.getLogger(AbsTask.class);
    protected final Attribute attr;
    protected final DataStorage storage;
    protected final boolean append;

    public AbsTask(Attribute attr, DataStorage storage, boolean append) {
        this.attr = attr;
        this.storage = storage;
        this.append = append;
    }
}
