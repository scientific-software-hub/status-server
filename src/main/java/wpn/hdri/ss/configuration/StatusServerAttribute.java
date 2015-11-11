package wpn.hdri.ss.configuration;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 21.03.13
 */
@Root(name = "attribute")
public class StatusServerAttribute {
    @Attribute(name = "name")
    private String name;
    @Attribute(name = "alias")
    private String alias;
    @Attribute(name = "type")
    private String type;

    public StatusServerAttribute(
            @Attribute(name = "name") String name,
            @Attribute(name = "alias") String alias,
            @Attribute(name = "type") String type) {
        this.name = name;
        this.alias = alias;
        this.type = type;
    }

    public String getAlias() {
        return alias;
    }

    public String getName() {
        return name;
    }

    public String getType(){
        return type;
    }

    public DeviceAttribute asDeviceAttribute(){
        DeviceAttribute deviceAttribute = new DeviceAttribute();
        deviceAttribute.setName(name);
        deviceAttribute.setAlias(alias);
        return deviceAttribute;
    }
}
