package wpn.hdri.ss.tango;

import wpn.hdri.ss.data2.Attribute;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 11.11.2015
 */
public class DefaultAttributesGroup extends AttributesGroup {
    public DefaultAttributesGroup() {
        super("default", null);
    }

    @Override
    public boolean hasAttribute(Attribute attr) {
        return true;
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    /**
     *
     * @return -1
     */
    @Override
    public int size() {
        return -1;
    }

    @Override
    public String toString() {
        return "DefaultAttributesGroup";
    }
}
