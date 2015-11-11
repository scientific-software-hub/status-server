package wpn.hdri.ss.engine;

import wpn.hdri.ss.configuration.StatusServerProperties;

import java.util.Collections;
import java.util.List;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.06.13
 */
public class EngineInitializationContext {
    public ClientsManager clientsManager;
    public AttributesManager attributesManager;
    public StatusServerProperties properties;
    public List<PollingReadAttributeTask> pollingTasks;
    public List<EventReadAttributeTask> eventTasks;

    public EngineInitializationContext(ClientsManager clientsManager, AttributesManager attributesManager, StatusServerProperties properties, List<PollingReadAttributeTask> pollingTasks, List<EventReadAttributeTask> eventTasks) {
        this.clientsManager = clientsManager;
        this.attributesManager = attributesManager;
        this.properties = properties;
        this.pollingTasks = Collections.unmodifiableList(pollingTasks);
        this.eventTasks = Collections.unmodifiableList(eventTasks);
    }
}
