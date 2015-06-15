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
    public static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
    public int engineCpus = AVAILABLE_PROCESSORS;
    public int jacorbMinCpus = 1;
    public int jacorbMaxCpus = 100;
    public long persistentThreshold = 1000;
    public long persistentDelay = 10;
    public String persistentRoot = ".";
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
                case "persistent.threshold":
                    persistentThreshold = Long.parseLong(prop.value);
                    break;
                case "persistent.delay":
                    persistentDelay = Long.parseLong(prop.value);
                    break;
                case "persistent.root":
                    persistentRoot = prop.value;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown property: " + prop.name);
            }
        }
    }
}
