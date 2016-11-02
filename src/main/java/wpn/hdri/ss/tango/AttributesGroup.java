package wpn.hdri.ss.tango;

import wpn.hdri.ss.data2.Attribute;

import java.util.Collection;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 11.11.2015
 */
public class AttributesGroup {
    public final String name;
    public final Collection<Attribute<?>> attributes;

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

    public int size(){
        return attributes.size();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("AttributesGroup@");
        result.append(hashCode())
                .append("[")
                .append(name)
                .append(";{");

        for(Attribute<?> attr : attributes){
            result.append(attr.name).append(",");
        }

        result.append("}]");
        return result.toString();
    }
}
