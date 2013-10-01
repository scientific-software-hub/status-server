package wpn.hdri.ss.data.attribute;

import javax.annotation.concurrent.ThreadSafe;

/**
 * This is a container class for attribute's full name and its alias.
 * This can be used as key in hashed collections. Full name is used in equals and hashCode
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 03.06.13
 */
@ThreadSafe
public class AttributeName {
    private final String deviceName;
    private final String name;
    private final String full;//combination of deviceName and name
    private final String alias;


    public AttributeName(String deviceName, String name, String alias) {
        this.deviceName = deviceName;
        this.name = name;
        this.full = deviceName + '/' + name;
        this.alias = alias;
    }

    /**
     * Creates instance with deviceName and name set to null
     *
     * @param fullName
     * @param alias
     */
    public AttributeName(String fullName, String alias) {
        this.deviceName = null;
        this.name = null;
        this.full = fullName;
        this.alias = alias;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getName() {
        return name;
    }

    /**
     * @return full name of this attribute, i.e. including device and protocol
     */
    public String getFullName() {
        return full;
    }

    public String getAlias() {
        return alias;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AttributeName that = (AttributeName) o;

        if (full != null ? !full.equals(that.full) : that.full != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return full != null ? full.hashCode() : 0;
    }

    @Override
    public String toString() {
        return full;
    }
}
