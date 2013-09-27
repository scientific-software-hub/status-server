package wpn.hdri.ss.engine;

import org.apache.log4j.Logger;
import wpn.hdri.ss.StatusServerProperties;
import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.client.ClientException;
import wpn.hdri.ss.client.ClientFactory;
import wpn.hdri.ss.configuration.Device;
import wpn.hdri.ss.configuration.DeviceAttribute;
import wpn.hdri.ss.configuration.StatusServerAttribute;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.data.Method;
import wpn.hdri.ss.data.Value;
import wpn.hdri.ss.data.attribute.Attribute;
import wpn.hdri.ss.data.attribute.AttributeFactory;
import wpn.hdri.ss.engine.exception.ClientInitializationException;
import wpn.hdri.tango.data.type.TangoDataType;
import wpn.hdri.tango.data.type.TangoDataTypes;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Encapsulates initialization logic of this engine.
 */
public class EngineInitializer {
    public static final Logger LOGGER = Logger.getLogger(EngineInitializer.class);

    private final StatusServerConfiguration configuration;
    private final StatusServerProperties properties;

    public EngineInitializer(StatusServerConfiguration configuration, StatusServerProperties properties) {
        this.configuration = configuration;
        this.properties = properties;
    }

    public EngineInitializationContext initialize() {
        LOGGER.info(new SimpleDateFormat("dd MMM yy HH:mm").format(new Date()) + " Engine initialization process started.");
        ClientsManager clientsManager = initializeClients();

        AttributesManager attributesManager = initializeAttributes(clientsManager);

        List<PollingReadAttributeTask> pollingTasks = initializePollTasks(clientsManager, attributesManager);
        List<EventReadAttributeTask> eventTasks = initializeEventTasks(clientsManager, attributesManager);

        PersistentStorageTask persistentStorageTask = new PersistentStorageTask(attributesManager, properties.persistentThreshold, properties.persistentRoot);

        LOGGER.info("Finish engine initialization process.");
        return new EngineInitializationContext(clientsManager, attributesManager, properties, pollingTasks, eventTasks, persistentStorageTask);
    }

    public ClientsManager initializeClients() {
        ClientsManager clientsManager = new ClientsManager(new ClientFactory());
        for (Device dev : configuration.getDevices()) {
            String devName = dev.getName();
            try {
                LOGGER.info("Initializing client " + devName);
                clientsManager.initializeClient(devName);
            } catch (ClientInitializationException e) {
                LOGGER.warn("Client initialization failed.", e);
                clientsManager.reportBadClient(devName, e.getMessage());
            }
        }

        return clientsManager;
    }

    /**
     * Creates attributes defined in the configuration. Upon creation adds the {@link Value#NULL} as the default value (without persisting it)
     *
     * @param clientsManager
     * @return a newly created {@link AttributesManager} instance filled with the attributes defined in the configuration. Attributes have default value set to {@link Value#NULL}
     */
    public AttributesManager initializeAttributes(ClientsManager clientsManager) {
        long now = System.currentTimeMillis();

        AttributesManager attributesManager = new AttributesManager(new AttributeFactory());
        for (Device dev : configuration.getDevices()) {
            String devName = dev.getName();

            final Client devClient = clientsManager.getClient(devName);

            for (DeviceAttribute attr : dev.getAttributes()) {
                final String fullName = devName + "/" + attr.getName();
                LOGGER.info("Initializing attribute " + fullName);
                boolean isAttrOk = devClient.checkAttribute(attr.getName());
                if (!isAttrOk) {
                    LOGGER.warn("DevClient reports bad attribute: " + fullName);
                    attributesManager.reportBadAttribute(fullName, "Attribute initialization failed.");
                    continue;
                }
                devClient.printAttributeInfo(attr.getName(), LOGGER);
                try {
                    Class<?> attributeClass = devClient.getAttributeClass(attr.getName());
                    boolean isArray = devClient.isArrayAttribute(attr.getName());
                    attributesManager.initializeAttribute(attr, dev.getName(), devClient, attributeClass, isArray);
                    LOGGER.info("Initialization succeed.");
                } catch (ClientException e) {
                    LOGGER.warn("Attribute initialization failed.", e);
                    attributesManager.reportBadAttribute(fullName, e.getMessage());
                }
            }
        }

        //initialize StatusServer embedded attributes
        for (StatusServerAttribute attr : configuration.getStatusServerAttributes()) {
            LOGGER.info("Initializing embedded attribute " + attr.getName());
            TangoDataType<?> dataType = TangoDataTypes.forString(attr.getType());
            //create and add default value - null
            attributesManager.initializeAttribute(attr.asDeviceAttribute(), "", null, dataType.getDataType(), false);
            LOGGER.info("Initialization succeed.");
        }
        return attributesManager;
    }

    public List<EventReadAttributeTask> initializeEventTasks(ClientsManager clientsManager, AttributesManager attributesManager) {
        List<EventReadAttributeTask> result = new ArrayList<EventReadAttributeTask>();
        for (Attribute<?> attribute : attributesManager.getAttributesByMethod(Method.EVENT)) {
            final Client devClient = clientsManager.getClient(attribute.getName().getDeviceName());
            result.add(new EventReadAttributeTask(attribute, devClient, true, LOGGER));
        }
        return result;
    }

    public List<PollingReadAttributeTask> initializePollTasks(ClientsManager clientsManager, AttributesManager attributesManager) {
        List<PollingReadAttributeTask> result = new ArrayList<PollingReadAttributeTask>();
        for (final Attribute<?> attribute : attributesManager.getAttributesByMethod(Method.POLL)) {
            DeviceAttribute attr = configuration.getDeviceAttribute(attribute.getName().getDeviceName(), attribute.getName().getName());
            final Client devClient = clientsManager.getClient(attribute.getName().getDeviceName());
            result.add(new PollingReadAttributeTask(attribute, devClient, attr.getDelay(), true, LOGGER));
        }
        return result;
    }
}
