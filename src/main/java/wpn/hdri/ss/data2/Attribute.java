package wpn.hdri.ss.data2;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import wpn.hdri.ss.client2.ClientAdaptor;
import wpn.hdri.ss.data.Method;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 09.11.2015
 */
public class Attribute<T> {
    public final int ndx;
    public final ClientAdaptor devClient;
    public final long delay;
    public final Method.EventType eventType;
    public final String fullName;
    public final String name;
    public final String alias;
    public final Class<T> type;

    public Attribute(int ndx, ClientAdaptor devClient, long delay, Method.EventType eventType, Class<T> type, String alias, String fullName, String name) {
        this.ndx = ndx;
        this.devClient = devClient;
        this.delay = delay;
        this.eventType = eventType;
        this.type = type;
        this.alias = alias;
        this.fullName = fullName;
        this.name = name;
    }

    public String toString(){
        return MoreObjects.toStringHelper(getClass())
                .add("ndx", ndx)
                .add("name", name)
                .add("fullName", fullName)
                .add("alias", alias)
                .add("type", type.getSimpleName())
                .add("delay", delay)
                .add("eventType", eventType).toString();
    }
}
