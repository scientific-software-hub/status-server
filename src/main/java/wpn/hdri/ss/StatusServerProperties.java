package wpn.hdri.ss;

import hzg.wpn.properties.Properties;
import hzg.wpn.properties.Property;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.06.13
 */
@Properties(file = "statusserver.properties")
public class StatusServerProperties {
    @Property("engine.thread_pool_max")
    public int engineCpus = Runtime.getRuntime().availableProcessors() * 2;
    @Property("jacorb.poa.thread_pool_min")
    public int jacorbMinCpus = Runtime.getRuntime().availableProcessors();
    @Property("jacorb.poa.thread_pool_max")
    public int jacorbMaxCpus = Runtime.getRuntime().availableProcessors() * 2;
    @Property("persistent.threshold")
    public long persistentThreshold;
    @Property("persistent.delay")
    public long persistentDelay;
    @Property("persistent.root")
    public String persistentRoot;
}
