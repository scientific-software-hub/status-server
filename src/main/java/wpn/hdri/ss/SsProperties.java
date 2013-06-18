package wpn.hdri.ss;

import hzg.wpn.properties.Properties;
import hzg.wpn.properties.Property;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.06.13
 */
@Properties("statusserver.properties")
public class SsProperties {
    @Property(key = "engine.thread_pool_max")
    public int engineCpus = Runtime.getRuntime().availableProcessors() * 2;
    @Property(key = "jacorb.poa.thread_pool_max")
    public int jacorbCpus = Runtime.getRuntime().availableProcessors() * 2;
}
