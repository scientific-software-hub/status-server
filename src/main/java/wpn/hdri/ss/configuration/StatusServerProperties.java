package wpn.hdri.ss.configuration;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

/**
 * StatusServer.configuration.xml
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.06.13
 */
@Root
public class StatusServerProperties {
    public int jacorbMinCpus = 1;
    public int jacorbMaxCpus = 100;
    @ElementList(name = "properties", inline = true)
    private List<StatusServerProperty> properties;

    public StatusServerProperties(@ElementList(name = "properties", inline = true) List<StatusServerProperty> properties) {
        this.properties = properties;
        for (StatusServerProperty prop : properties) {
            switch (prop.name) {
                case "jacorb.poa.thread_pool_min":
                    jacorbMinCpus = Integer.parseInt(prop.value);
                    break;
                case "jacorb.poa.thread_pool_max":
                    jacorbMaxCpus = Integer.parseInt(prop.value);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown property: " + prop.name);
            }
        }
    }
}
