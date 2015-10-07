package wpn.hdri.ss.configuration;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

/**
 * XML property tag Java representation
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 21.03.2015
 */
@Root(name = "property")
public class StatusServerProperty {
    @Attribute(name = "name")
    String name;
    @Attribute(name = "value")
    String value;

    public StatusServerProperty(@Attribute(name = "name") String name,
                                @Attribute(name = "value") String value) {
        this.name = name;
        this.value = value;
    }
}
