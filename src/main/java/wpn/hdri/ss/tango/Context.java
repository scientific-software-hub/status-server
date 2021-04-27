package wpn.hdri.ss.tango;

import com.google.common.base.MoreObjects;
import wpn.hdri.ss.data2.Attribute;

import java.util.Collection;

/**
* @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
* @since 11.11.2015
*/
public class Context {
    public final String cid;
    public volatile boolean useAliases = false;
    public volatile boolean encode = false;
    public volatile OutputType outputType = OutputType.PLAIN;
    public volatile long lastTimestamp;
    public final ContextManager contextManager;

    /**
     * Creates default context
     *
     * @param cid
     * @param attributes
     * @param contextManager
     */
    public Context(String cid, Collection<Attribute<?>> attributes, ContextManager contextManager) {
        this.cid = cid;
        this.contextManager = contextManager;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("cid", cid)
                .add("useAliases", useAliases)
                .add("encode", encode)
                .add("outputType", outputType)
                .add("lastTimestamp", lastTimestamp)
                .toString();
    }


    public ContextManager getContextManager() {
        return contextManager;
    }
}
