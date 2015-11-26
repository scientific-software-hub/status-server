package wpn.hdri.ss.tango;

import wpn.hdri.ss.data2.Attribute;

import java.util.*;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 11.11.2015
 */
public class AttributesGroup {
    public final String name;
    private final Collection<Attribute<?>> attributes;

    public AttributesGroup(String name, Collection<Attribute<?>> attributes) {
        this.name = name;
        this.attributes = attributes;
    }

    public boolean hasAttribute(Attribute attr){
        return attributes.contains(attr);
    }

    public boolean isDefault() {
        return false;
    }
}
