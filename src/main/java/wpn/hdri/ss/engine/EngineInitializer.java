package wpn.hdri.ss.engine;

import org.apache.log4j.Logger;
import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.client.ClientException;
import wpn.hdri.ss.client.ClientFactory;
import wpn.hdri.ss.configuration.Device;
import wpn.hdri.ss.configuration.DeviceAttribute;
import wpn.hdri.ss.configuration.StatusServerAttribute;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.data.Attribute;
import wpn.hdri.ss.data.AttributeFactory;
import wpn.hdri.ss.data.Method;
import wpn.hdri.ss.storage.StorageFactory;
import wpn.hdri.tango.data.type.TangoDataType;
import wpn.hdri.tango.data.type.TangoDataTypes;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Encapsulates initialization logic of this engine.
 */
public class EngineInitializer {
    public static final Logger LOGGER = Logger.getLogger(EngineInitializer.class);

    private final StatusServerConfiguration configuration;
    //TODO replace with type safe class
    private final Properties properties;

    public EngineInitializer(StatusServerConfiguration configuration, Properties properties) {
        this.configuration = configuration;
        this.properties = properties;
    }

    private Engine initialize() {
        LOGGER.info(new SimpleDateFormat("dd MMM yy HH:mm").format(new Date()) + " Engine initialization process started.");
        //TODO pass to AttributesManager
        StorageFactory storageFactory = new StorageFactory(/*TODO type*/);

        ClientsManager clientsManager = initializeClients();

        AttributesManager attributesManager = initializeAttributes(clientsManager);

        List<PollingReadAttributeTask> pollingTasks =  initializePollTasks(clientsManager,attributesManager);
        List<EventReadAttributeTask> eventTasks   = initializeEventTasks(clientsManager,attributesManager);

        Engine engine = new Engine(clientsManager, attributesManager, Integer.parseInt(properties.getProperty("CPUS")));
        engine.submitPollingTasks(pollingTasks);
        engine.submitEventTasks(eventTasks);
        LOGGER.info("Finish engine initialization process.");
        return engine;
    }

    private ClientsManager initializeClients() {
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

    private AttributesManager initializeAttributes(ClientsManager clientsManager) {
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
            attributesManager.initializeAttribute(attr.asDeviceAttribute(), "", null, dataType.getDataType(), false);
            LOGGER.info("Initialization succeed.");
        }
        return attributesManager;
    }

    private List<EventReadAttributeTask> initializeEventTasks(ClientsManager clientsManager, AttributesManager attributesManager) {
        List<EventReadAttributeTask> result = new ArrayList<EventReadAttributeTask>();
        for (Attribute<?> attribute : attributesManager.getAttributesByMethod(Method.EVENT)) {
            final Client devClient = clientsManager.getClient(attribute.getName().getDeviceName());
            result.add(new EventReadAttributeTask(attribute, devClient, true, LOGGER));
        }
        return result;
    }

    private List<PollingReadAttributeTask> initializePollTasks(ClientsManager clientsManager, AttributesManager attributesManager) {
        List<PollingReadAttributeTask> result = new ArrayList<PollingReadAttributeTask>();
        for (final Attribute<?> attribute : attributesManager.getAttributesByMethod(Method.POLL)) {
            DeviceAttribute attr = configuration.getDeviceAttribute(attribute.getName().getDeviceName(), attribute.getName().getName());
            final Client devClient = clientsManager.getClient(attribute.getName().getDeviceName());
            result.add(new PollingReadAttributeTask(attribute, devClient, attr.getDelay(), true, LOGGER));
        }
        return result;
    }
}
