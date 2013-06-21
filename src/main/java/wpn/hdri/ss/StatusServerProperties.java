package wpn.hdri.ss;

import hzg.wpn.properties.Properties;
import hzg.wpn.properties.Property;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.06.13
 */
@Properties(file = "statusserver.properties")
public class StatusServerProperties {
    public static final String ENGINE_PERSISTENT_ROOT = "engine.persistent_root";

    @Property(key = "engine.thread_pool_max")
    public int engineCpus = Runtime.getRuntime().availableProcessors() * 2;
    @Property(key = ENGINE_PERSISTENT_ROOT)
    public String engineStorageRoot = System.getProperty("user.dir");
    @Property(key = "engine.persistent_max")
    public long engineStorageMax = 100000;
    @Property(key = "engine.persistent_split")
    public long engineStorageSplit = 50000;
    @Property(key = "jacorb.poa.thread_pool_min")
    public int jacorbMinCpus = Runtime.getRuntime().availableProcessors();
    @Property(key = "jacorb.poa.thread_pool_max")
    public int jacorbMaxCpus = Runtime.getRuntime().availableProcessors() * 2;
}
