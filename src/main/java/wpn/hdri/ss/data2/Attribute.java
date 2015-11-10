package wpn.hdri.ss.data2;

import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.client2.ClientAdaptor;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 09.11.2015
 */
public class Attribute {
    public final int ndx;
    public final ClientAdaptor devClient;
    public final long delay;
    public final String fullName;
    public final String name;
    public final String alias;
    public final Class<?> type;

    public Attribute(int ndx, ClientAdaptor devClient, long delay, Class<?> type, String alias, String fullName, String name) {
        this.ndx = ndx;
        this.devClient = devClient;
        this.delay = delay;
        this.type = type;
        this.alias = alias;
        this.fullName = fullName;
        this.name = name;
    }
}
