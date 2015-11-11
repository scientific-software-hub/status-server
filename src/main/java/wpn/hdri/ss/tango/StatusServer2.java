package wpn.hdri.ss.tango;

import fr.esrf.Tango.AttrDataFormat;
import fr.esrf.Tango.AttrWriteType;
import fr.esrf.Tango.DevFailed;
import hzg.wpn.xenv.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tango.client.ez.data.type.TangoDataType;
import org.tango.client.ez.data.type.TangoDataTypes;
import org.tango.client.ez.data.type.UnknownTangoDataType;
import org.tango.server.ServerManager;
import org.tango.server.StateMachineBehavior;
import org.tango.server.annotation.*;
import org.tango.server.attribute.AttributeConfiguration;
import org.tango.server.attribute.AttributeValue;
import org.tango.server.attribute.IAttributeBehavior;
import org.tango.server.device.DeviceManager;
import org.tango.server.dynamic.DynamicManager;
import wpn.hdri.ss.configuration.StatusServerAttribute;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.configuration.StatusServerProperties;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.engine2.Engine;
import wpn.hdri.ss.engine2.EngineFactory;

import java.io.InputStream;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 11.11.2015
 */
@Device
public class StatusServer2 {
    private static final Logger logger = LoggerFactory.getLogger(StatusServer2.class);

    @DeviceManagement
    private DeviceManager deviceManager;

    public void setDeviceManager(DeviceManager deviceManager){
        this.deviceManager = deviceManager;
    }

    @DynamicManagement
    private DynamicManager dynamicManager;

    public void setDynamicManager(DynamicManager manager){
        this.dynamicManager = manager;
    }


    @Status
    private String status;

    public String getStatus(){
        return status;
    }

    public void setStatus(String status){
        this.status = status;
    }


    private Engine engine;

    @Init
    public void init() throws Exception {
        String devName = deviceManager.getName().split("/")[2];

        InputStream xmlStream = ResourceManager.loadResource("etc/StatusServer", devName + ".xml");

        logger.info("Loading configuration...");
        StatusServerConfiguration configuration = StatusServerConfiguration.fromXmlStream(xmlStream);
        logger.info("Done.");

        StatusServerProperties properties = configuration.getProperties();

        logger.info("Tuning jacORB thread pool:");
        logger.info("Setting System settings...");
        logger.info("jacorb.poa.thread_pool_min={}", properties.jacorbMinCpus);
        System.setProperty("jacorb.poa.thread_pool_min", Integer.toString(properties.jacorbMinCpus));

        logger.info("jacorb.poa.thread_pool_max={}", properties.jacorbMaxCpus);
        System.setProperty("jacorb.poa.thread_pool_max", Integer.toString(properties.jacorbMaxCpus));
        logger.info("Done.");

        initializeStatusServerAttributes(configuration, dynamicManager);

        EngineFactory engineFactory = new EngineFactory(configuration);
        this.engine = engineFactory.newEngine();
        setStatus(StatusServerStatus.IDLE);
    }

    private void initializeStatusServerAttributes(StatusServerConfiguration configuration, DynamicManager dynamicManagement) throws DevFailed {
        final AtomicInteger attrNdx = new AtomicInteger(0);
        for (final Iterator<StatusServerAttribute> iterator = configuration.getStatusServerAttributes().iterator(); iterator.hasNext(); ) {
            final StatusServerAttribute attribute = iterator.next();
            final AttributeConfiguration attributeConfiguration = new AttributeConfiguration();
            try {
                attributeConfiguration.setName(attribute.getName());
                TangoDataType<?> dataType = TangoDataTypes.forString(attribute.getType());
                attributeConfiguration.setTangoType(dataType.getAlias(), AttrDataFormat.SCALAR);
                attributeConfiguration.setType(dataType.getDataType());
                attributeConfiguration.setWritable(AttrWriteType.READ_WRITE);
            } catch (UnknownTangoDataType unknownTangoDataType) {
                logger.error("Cannot initialize StatusServer attribute[{}] of unknown type[{}]",
                        attribute.getName(), attribute.getType());
                iterator.remove();
                continue;
            }
            dynamicManagement.addAttribute(new IAttributeBehavior() {
                private final int ndx = attrNdx.getAndIncrement();
                private volatile AttributeValue value;

                @Override
                public AttributeConfiguration getConfiguration() throws DevFailed {
                    return attributeConfiguration;
                }

                @Override
                public AttributeValue getValue() throws DevFailed {
                    return value;
                }

                @Override
                public void setValue(AttributeValue value) throws DevFailed {
                    this.value = value;
                    SingleRecord<Object> record = new SingleRecord<>(ndx, System.currentTimeMillis(), value.getTime(), value.getValue());
                    if(StatusServer2.this.getStatus() == StatusServerStatus.HEAVY_DUTY) {
                        engine.getStorage().appendRecord(record);
                    } else {
                        engine.getStorage().writeRecord(record);
                    }

                }

                @Override
                public StateMachineBehavior getStateMachine() throws DevFailed {
                    return new StateMachineBehavior();
                }
            });
        }
    }


    public static void main(String[] args) {
        ServerManager.getInstance().start(args, StatusServer2.class);
    }
}
