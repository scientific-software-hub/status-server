package wpn.hdri.ss.data2;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 09.11.2015
 */
public class Attribute {
    public final String fullName;
    public final String alias;
    public final Class<?> type;

    public Attribute(Class<?> type, String alias, String fullName) {
        this.type = type;
        this.alias = alias;
        this.fullName = fullName;
    }
}
