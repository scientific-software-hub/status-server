package wpn.hdri.ss.engine;

import wpn.hdri.ss.StatusServerProperties;

import java.util.List;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.06.13
 */
public class EngineInitializationContext {
    public final ClientsManager clientsManager;
    public final AttributesManager attributesManager;
    public final StatusServerProperties properties;
    public final List<PollingReadAttributeTask> pollingTasks;
    public final List<EventReadAttributeTask> eventTasks;
    public final PersistentStorageTask persistentStorageTask;

    public EngineInitializationContext(ClientsManager clientsManager, AttributesManager attributesManager, StatusServerProperties properties, List<PollingReadAttributeTask> pollingTasks, List<EventReadAttributeTask> eventTasks, PersistentStorageTask persistentStorageTask) {
        this.clientsManager = clientsManager;
        this.attributesManager = attributesManager;
        this.properties = properties;
        this.pollingTasks = pollingTasks;
        this.eventTasks = eventTasks;
        this.persistentStorageTask = persistentStorageTask;
    }
}
