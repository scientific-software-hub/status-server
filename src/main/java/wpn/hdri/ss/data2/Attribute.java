package wpn.hdri.ss.data2;

import com.google.common.base.MoreObjects;
import wpn.hdri.ss.client2.ClientAdaptor;
import wpn.hdri.ss.data.Method;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 09.11.2015
 */
public class Attribute<T> {
    public final int id;
    public final ClientAdaptor devClient;
    public final long delay;
    public final Method.EventType eventType;
    public final String fullName;
    public final String name;
    public final String alias;
    public final Class<T> type;
    public final Interpolation interpolation;

    public Attribute(int id, ClientAdaptor devClient, long delay, Method.EventType eventType, Class<T> type, String alias, String fullName, String name, Interpolation interpolation) {
        this.id = id;
        this.devClient = devClient;
        this.delay = delay;
        this.eventType = eventType;
        this.type = type;
        this.alias = alias;
        this.fullName = fullName;
        this.name = name;
        this.interpolation = interpolation;
    }

    /**
     * For testing
     *
     * @param id
     */
    Attribute(int id){
        this(id, null, 0L, null, null, null, null, null, Interpolation.LAST);
    }

    public String toString(){
        return MoreObjects.toStringHelper(getClass())
                .add("id", id)
                .add("name", name)
                .add("fullName", fullName)
                .add("alias", alias)
                .add("type", type.getSimpleName())
                .add("delay", delay)
                .add("eventType", eventType)
                .add("interpolation", interpolation).toString();
    }
}
