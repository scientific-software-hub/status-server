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

    /**
     * Creates default context
     * @param cid
     * @param attributes
     */
    public Context(String cid, Collection<Attribute<?>> attributes) {
        this.cid = cid;
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


}
