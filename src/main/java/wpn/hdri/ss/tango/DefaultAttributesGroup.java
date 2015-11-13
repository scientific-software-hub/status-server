package wpn.hdri.ss.tango;

import wpn.hdri.ss.data2.Attribute;

import java.util.List;

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
}
