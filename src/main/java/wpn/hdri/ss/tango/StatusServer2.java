package wpn.hdri.ss.tango;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import fr.esrf.Tango.AttrDataFormat;
import fr.esrf.Tango.AttrWriteType;
import fr.esrf.Tango.DevFailed;
import hzg.wpn.xenv.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tango.DeviceState;
import org.tango.client.ez.data.type.TangoDataType;
import org.tango.client.ez.data.type.TangoDataTypes;
import org.tango.client.ez.data.type.UnknownTangoDataType;
import org.tango.server.InvocationContext;
import org.tango.server.ServerManager;
import org.tango.server.StateMachineBehavior;
import org.tango.server.annotation.*;
import org.tango.server.annotation.Attribute;
import org.tango.server.attribute.AttributeConfiguration;
import org.tango.server.attribute.AttributeValue;
import org.tango.server.attribute.IAttributeBehavior;
import org.tango.server.device.DeviceManager;
import org.tango.server.dynamic.DynamicManager;
import org.tango.utils.ClientIDUtil;
import wpn.hdri.ss.configuration.StatusServerAttribute;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.configuration.StatusServerProperties;
import wpn.hdri.ss.data.Method;
import wpn.hdri.ss.data2.*;
import wpn.hdri.ss.engine2.Engine;
import wpn.hdri.ss.engine2.EngineFactory;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.lang.String;
import java.util.*;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 11.11.2015
 */
@Device(deviceType = "xenv.StatusServer", transactionType = TransactionType.NONE)
public class StatusServer2 {
    private static final Logger logger = LoggerFactory.getLogger(StatusServer2.class);

    @DeviceManagement
    private DeviceManager deviceManager;

    public void setDeviceManager(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    @DynamicManagement
    private DynamicManager dynamicManager;

    public void setDynamicManager(DynamicManager manager) {
        this.dynamicManager = manager;
    }


    @Status
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    private Engine engine;

    private final ContextManager contextManager = new ContextManager();

    @Attribute
    public boolean getUseAliases() {
        return contextManager.getContext().useAliases;
    }

    @Attribute
    public void setUseAliases(boolean isUseAliases) {
        contextManager.getContext().useAliases = isUseAliases;
    }

    @Attribute
    public boolean getEncode() {
        return contextManager.getContext().encode;
    }

    @Attribute
    public void setEncode(boolean encode) {
        contextManager.getContext().encode = encode;
    }

    @Attribute
    public String getOutputType() {
        return contextManager.getContext().outputType.name();
    }

    @Attribute
    public void setOutputType(String output) {
        contextManager.getContext().outputType = OutputType.valueOf(output.toUpperCase());
    }

    @Attribute
    public String getGroup() {
        return contextManager.getContext().attributesGroup.name;
    }

    @Attribute
    public void setGroup(String attributesGroup) {
        Context context = contextManager.getContext();
        if (context.hasGroup(attributesGroup))
            context.attributesGroup = context.getGroup(attributesGroup);
        else
            throw new IllegalArgumentException("AttributesGroup[" + attributesGroup + "] does not exist!");
    }

    @Attribute
    public String[] getGroups(){
        Context context = contextManager.getContext();
        return Iterables.toArray(context.getGroups(), String.class);
    }

    @Attribute
    public String getImplementationVersion(){
        return getClass().getPackage().getImplementationVersion();
    }


    @AroundInvoke
    public void aroundInvoke(InvocationContext invocationContext) {
        contextManager.setClientId(ClientIDUtil.toString(invocationContext.getClientID()));
    }

    @Attribute
    public String getClientId() {
        return contextManager.getClientId();
    }

    @Command
    public void createAttributesGroup(String[] attributeNames) {
        if (attributeNames.length < 2) throw new IllegalArgumentException("At least two elements are expected here!");
        String groupName = attributeNames[0];

        Collection<wpn.hdri.ss.data2.Attribute<?>> attributes = Collections2.transform(Arrays.asList(attributeNames).subList(1, attributeNames.length), new Function<String, wpn.hdri.ss.data2.Attribute<?>>() {
            @Nullable
            @Override
            public wpn.hdri.ss.data2.Attribute<?> apply(@Nullable String input) {
                return engine.getAttributeByName(input);
            }
        });

        contextManager.getContext().setGroup(new AttributesGroup(groupName, attributes));
    }

    @Command
    public String[] getDataRange(long[] t){
        if(t.length != 2) throw new IllegalArgumentException("Exactly two arguments are expected here: t0&t1");
        if(t[0] >= t[1]) throw new IllegalArgumentException("t0 must be LT t1");

        Context context = contextManager.getContext();

        return snapshotToStrings(engine.getStorage().getAllRecords().getRange(t[0], t[1]), context);
    }

    @Command
    public String[] getLatestSnapshot() {
        Context ctx = contextManager.getContext();

        return snapshotToStrings(engine.getStorage().getSnapshot(), ctx);
    }

    @Command
    public String[] getSnapshot(long t){
        Context context = contextManager.getContext();

        return snapshotToStrings(engine.getStorage().getAllRecords().getRange(t), context);
    }

    @Command
    @StateMachine(endState = DeviceState.RUNNING, deniedStates = {DeviceState.RUNNING})
    public void start() {
        engine.start();
        setStatus(StatusServerStatus.HEAVY_DUTY);
    }

    @Command
    @StateMachine(endState = DeviceState.RUNNING, deniedStates = {DeviceState.RUNNING})
    public void startLightPoling() {
        engine.startLightPolling();
        setStatus(StatusServerStatus.LIGHT_POLLING);
    }

    @Command
    @StateMachine(endState = DeviceState.RUNNING, deniedStates = {DeviceState.RUNNING})
    public void startLightPolingAtFixedRate(long delay) {
        engine.startLightPollingAtFixedRate(delay);
        setStatus(StatusServerStatus.LIGHT_POLLING_AT_FIXED_RATE);
    }

    @Command
    @StateMachine(endState = DeviceState.ON, deniedStates = DeviceState.ON)
    public void stop() {
        engine.stop();
        setStatus(StatusServerStatus.IDLE);
    }

    @Init
    @StateMachine(endState = DeviceState.ON, deniedStates = DeviceState.RUNNING)
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

        List<wpn.hdri.ss.data2.Attribute<?>> selfAttributes = initializeStatusServerAttributes(configuration, dynamicManager);

        EngineFactory engineFactory = new EngineFactory(selfAttributes, configuration);
        this.engine = engineFactory.newEngine();
        setStatus(StatusServerStatus.IDLE);
    }

    private List<wpn.hdri.ss.data2.Attribute<?>> initializeStatusServerAttributes(StatusServerConfiguration configuration, DynamicManager dynamicManagement) throws DevFailed {
        int ndx = 0;
        List<wpn.hdri.ss.data2.Attribute<?>> result = new ArrayList<>();
        for (final Iterator<StatusServerAttribute> iterator = configuration.getStatusServerAttributes().iterator(); iterator.hasNext(); ) {
            final StatusServerAttribute attribute = iterator.next();
            final AttributeConfiguration attributeConfiguration = new AttributeConfiguration();
            Class<?> type = null;
            try {
                attributeConfiguration.setName(attribute.getName());
                TangoDataType<?> dataType = TangoDataTypes.forString(attribute.getType());
                attributeConfiguration.setTangoType(dataType.getAlias(), AttrDataFormat.SCALAR);
                type = dataType.getDataType();
                attributeConfiguration.setType(type);
                attributeConfiguration.setWritable(AttrWriteType.READ_WRITE);
            } catch (UnknownTangoDataType unknownTangoDataType) {
                logger.error("Cannot initialize StatusServer attribute[{}] of unknown type[{}]",
                        attribute.getName(), attribute.getType());
                iterator.remove();
                continue;
            }

            final wpn.hdri.ss.data2.Attribute<?> attr = new wpn.hdri.ss.data2.Attribute<>(
                    ndx, null, 0L, Method.EventType.NONE, type, attribute.getAlias(), deviceManager.getName() + "/" + attribute.getName(), attribute.getName(), Interpolation.LAST);
            result.add(attr);
            dynamicManagement.addAttribute(new IAttributeBehavior() {
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
                    SingleRecord<Object> record = new SingleRecord(attr, System.currentTimeMillis(), value.getTime(), value.getValue());
                    if (StatusServer2.this.getStatus() == StatusServerStatus.HEAVY_DUTY) {
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
        return result;
    }


    public static void main(String[] args) {
        ServerManager.getInstance().start(args, StatusServer2.class);
    }
        private static String[] snapshotToStrings(Iterable<SingleRecord<?>> snapshot, final Context ctx) {
            Iterable<SingleRecord<?>> filtered = ctx.attributesGroup.isDefault() ?
                    snapshot :
                    Iterables.filter(snapshot, new Predicate<SingleRecord<?>>() {
                        @Override
                        public boolean apply(SingleRecord<?> input) {
                            if (input == null) return false;
                            return ctx.attributesGroup.hasAttribute(input.attribute);
                        }
                    });
            return (String[]) ctx.outputType.toType(filtered, ctx.useAliases);
    }
}
