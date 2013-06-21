package wpn.hdri.ss.data;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 21.06.13
 */
public class AttributeValuesStorageFactory {
    private final String storageRoot;
    private final long persistentMax;
    private final long persistentSplit;

    //TODO persistent storage type

    public AttributeValuesStorageFactory(String storageRoot, long persistentMax, long persistentSplit) {
        this.storageRoot = storageRoot;
        this.persistentMax = persistentMax;
        this.persistentSplit = persistentSplit;
    }

    public <T> AttributeValuesStorage<T> createInstance(String name){
        return new AttributeValuesStorage<T>(name,storageRoot,persistentMax,persistentSplit);
    }
}
