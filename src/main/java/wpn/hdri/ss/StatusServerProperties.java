package wpn.hdri.ss;

import hzg.wpn.properties.Properties;
import hzg.wpn.properties.Property;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.06.13
 */
@Properties(file = "StatusServer.properties")
public class StatusServerProperties {
    @Property("engine.thread_pool_max")
    public int engineCpus = Runtime.getRuntime().availableProcessors() * 2;
    @Property("jacorb.poa.thread_pool_min")
    public int jacorbMinCpus = 1;
    @Property("jacorb.poa.thread_pool_max")
    public int jacorbMaxCpus = Runtime.getRuntime().availableProcessors() * 2;
    @Property("persistent.threshold")
    public long persistentThreshold = 100;
    @Property("persistent.delay")
    public long persistentDelay = 10;
    @Property("persistent.root")
    public String persistentRoot = ".";
}
