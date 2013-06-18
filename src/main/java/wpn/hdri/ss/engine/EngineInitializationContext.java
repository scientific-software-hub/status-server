package wpn.hdri.ss.engine;

import java.util.List;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.06.13
 */
public class EngineInitializationContext {
    public final ClientsManager clientsManager;
    public final AttributesManager attributesManager;
    public final List<PollingReadAttributeTask> pollingTasks;
    public final List<EventReadAttributeTask> eventTasks;

    public EngineInitializationContext(ClientsManager clientsManager, AttributesManager attributesManager, List<PollingReadAttributeTask> pollingTasks, List<EventReadAttributeTask> eventTasks) {
        this.clientsManager = clientsManager;
        this.attributesManager = attributesManager;
        this.pollingTasks = pollingTasks;
        this.eventTasks = eventTasks;
    }
}
