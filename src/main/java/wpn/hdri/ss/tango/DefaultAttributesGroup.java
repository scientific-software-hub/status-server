package wpn.hdri.ss.tango;

import wpn.hdri.ss.data2.Attribute;

import java.util.Collection;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 11.11.2015
 */
public class DefaultAttributesGroup extends AttributesGroup {
    public DefaultAttributesGroup(Collection<Attribute<?>> attributes) {
        super("default", attributes);
    }

    /**
     *
     * @param attr
     * @return always true
     */
    @Override
    public boolean hasAttribute(Attribute attr) {
        return true;
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    @Override
    public String toString() {
        return "DefaultAttributesGroup";
    }
}
