package wpn.hdri.ss.tango;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Longs;
import fr.esrf.Tango.*;
import hzg.wpn.properties.PropertiesParser;
import hzg.wpn.util.compressor.Compressor;
import org.tango.DeviceState;
import org.tango.client.ez.data.type.TangoDataType;
import org.tango.client.ez.data.type.TangoDataTypes;
import org.tango.client.ez.data.type.UnknownTangoDataType;
import org.tango.server.ServerManager;
import org.tango.server.StateMachineBehavior;
import org.tango.server.annotation.*;
import org.tango.server.annotation.Device;
import org.tango.server.attribute.AttributeConfiguration;
import org.tango.server.attribute.IAttributeBehavior;
import org.tango.server.device.DeviceManager;
import org.tango.server.dynamic.DynamicManager;
import wpn.hdri.ss.StatusServerProperties;
import wpn.hdri.ss.configuration.StatusServerAttribute;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.data.attribute.AttributeName;
import wpn.hdri.ss.data.attribute.AttributeValue;
import wpn.hdri.ss.data.attribute.AttributeValuesView;
import wpn.hdri.ss.engine.AttributesManager;
import wpn.hdri.ss.engine.Engine;
import wpn.hdri.ss.engine.EngineInitializationContext;
import wpn.hdri.ss.engine.EngineInitializer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * StatusServer Tango implementation based on the new JTangoServer library
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 17.06.13
 */
@Device(transactionType = TransactionType.NONE)
public class StatusServer implements StatusServerStub {
    private static String XML_CONFIG_PATH;
    private final Multimap<String, String> attributesGroupsMap = HashMultimap.create();

    public static void setXmlConfigPath(String v) {
        XML_CONFIG_PATH = v;
    }

    private static interface Status {
        String IDLE = "IDLE";
        String LIGHT_POLLING = "LIGHT_POLLING";
        String LIGHT_POLLING_AT_FIXED_RATE = "LIGHT_POLLING_AT_FIXED_RATE";
        String HEAVY_DUTY = "HEAVY_DUTY";
    }

    /**
     * This field tracks ctxs of the clients and is used in getXXXUpdates methods
     */
    private final ConcurrentMap<String, RequestContext> ctxs = Maps.newConcurrentMap();


    private Engine engine;

    public StatusServer() {
        System.out.println("Create instance");
    }

    /**
     * For tests
     *
     * @param engine
     */
    StatusServer(Engine engine) {
        this.engine = engine;
    }

    // ==== Tango API specific
    @State
    private DeviceState state = DeviceState.OFF;

    @Override
    public DeviceState getState() {
        return state;
    }

    public void setState(DeviceState state) {
        this.state = state;
    }

    @org.tango.server.annotation.Status
    private String status = Status.IDLE;

    public void setStatus(String v) {
        this.status = v;
    }

    @Override
    public String getStatus() {
        return this.status;
    }

    @DynamicManagement
    private DynamicManager dynamicManagement;

    public void setDynamicManagement(DynamicManager dynamicManagement) {
        this.dynamicManagement = dynamicManagement;
    }

    @DeviceManagement
    private DeviceManager deviceManager;

    public void setDeviceManager(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }
    // ====================

    @Override
    @Init
    @StateMachine(endState = DeviceState.ON)
    public void init() throws Exception {
        Preconditions.checkNotNull(XML_CONFIG_PATH, "Path to xml configuration is not set.");
        StatusServerConfiguration configuration = StatusServerConfiguration.fromXml(XML_CONFIG_PATH);

        StatusServerProperties properties = PropertiesParser.createInstance(StatusServerProperties.class).parseProperties();

        EngineInitializer initializer = new EngineInitializer(configuration, properties);

        initializeDynamicAttributes(configuration.getStatusServerAttributes(), dynamicManagement);

        EngineInitializationContext ctx = initializer.initialize();

        this.engine = new Engine(ctx);
        setStatus(Status.IDLE);
    }

    private void initializeDynamicAttributes(List<StatusServerAttribute> statusServerAttributes, DynamicManager dynamicManagement) throws DevFailed {
        for (final StatusServerAttribute attribute : statusServerAttributes) {
            dynamicManagement.addAttribute(new IAttributeBehavior() {
                private final StatusServerAttribute wrapped = attribute;
                private final AtomicReference<org.tango.server.attribute.AttributeValue> value =
                        new AtomicReference<org.tango.server.attribute.AttributeValue>(new org.tango.server.attribute.AttributeValue(null));

                @Override
                public AttributeConfiguration getConfiguration() throws DevFailed {
                    AttributeConfiguration configuration = new AttributeConfiguration();
                    configuration.setName(wrapped.getName());
                    try {
                        TangoDataType<?> dataType = TangoDataTypes.forString(wrapped.getType());
                        configuration.setTangoType(dataType.getAlias(), AttrDataFormat.SCALAR);
                        configuration.setType(dataType.getDataType());
                        configuration.setWritable(AttrWriteType.READ_WRITE);
                        return configuration;
                    } catch (UnknownTangoDataType unknownTangoDataType) {
                        throw new RuntimeException(unknownTangoDataType);
                    }
                }

                @Override
                public org.tango.server.attribute.AttributeValue getValue() throws DevFailed {
                    return value.get();
                }

                @Override
                public void setValue(org.tango.server.attribute.AttributeValue value) throws DevFailed {
                    this.value.set(value);
                    //fix NPE by adding "/" in the beginning. See AttributeName#getFullName
                    engine.writeAttributeValue("/" + wrapped.getName(), this.value.get().getValue(), new Timestamp(value.getTime()));
                }

                @Override
                public StateMachineBehavior getStateMachine() throws DevFailed {
                    return new StateMachineBehavior();
                }
            });
        }
    }

    @Attribute
    public void setGroup(String attributesGroup) throws Exception {
        String cid = getClientId();
        Preconditions.checkArgument(attributesGroupsMap.get(cid).contains(attributesGroup), "No such group exists: " + attributesGroup);

        RequestContext ctx = getContext();
        if (attributesGroup.equals(AttributesManager.DEFAULT_ATTR_GROUP))
            attributesGroup = AttributesManager.DEFAULT_ATTR_GROUP;
        RequestContext updated = new RequestContext(ctx.useAliases, ctx.encode, ctx.outputType, ctx.lastTimestamp, attributesGroup);
        setContext(updated);
    }

    @Attribute
    public String getGroup() throws Exception {
        RequestContext cxt = getContext();
        return cxt.attributesGroup;
    }

    //TODO attributes
    @Attribute
    @Override
    public void setUseAliases(boolean v) throws Exception {
        RequestContext old = getContext();
        RequestContext ctx = new RequestContext(v, old.encode, old.outputType, old.lastTimestamp, old.attributesGroup);
        setContext(ctx);
    }

    private boolean isUseAliases() throws Exception {
        return getContext().useAliases;
    }

    @Override
    @Attribute
    public long getCrtTimestamp() {
        return System.currentTimeMillis();
    }

    @Attribute
    @Override
    public void setEncode(boolean encode) throws Exception {
        RequestContext ctx = getContext();
        RequestContext updated = new RequestContext(ctx.useAliases, encode, ctx.outputType, ctx.lastTimestamp, ctx.attributesGroup);
        setContext(updated);
    }

    @Attribute
    @AttributeProperties(description = "defines output type. Valid values are: PLAIN, JSON")
    @Override
    public void setOutputType(String outputType) throws Exception {
        OutputType type = OutputType.valueOf(outputType.toUpperCase());
        RequestContext ctx = getContext();
        RequestContext updated = new RequestContext(ctx.useAliases, ctx.encode, type, ctx.lastTimestamp, ctx.attributesGroup);
        setContext(updated);
    }

    @Override
    @Attribute
    public String[] getData() throws Exception {
        RequestContext ctx = getContext();
        Multimap<AttributeName, AttributeValue<?>> attributes = engine.getAllAttributeValues(null, ctx.attributesGroup);

        AttributeValuesView view = new AttributeValuesView(attributes, ctx.useAliases);
        return processResult(view);
    }

    private String[] processResult(AttributeValuesView view) throws Exception {
        RequestContext ctx = getContext();
        String[] result = new String[0];
        switch (ctx.outputType) {
            case PLAIN:
                result = view.toStringArray();
                break;
            case JSON:
                result = new String[]{view.toJsonString()};
                break;
        }
        if (ctx.encode) {
            for (int i = 0, resultLength = result.length; i < resultLength; i++) {
                String string = result[i];

                result[i] = new String(Compressor.encodeAndCompress(string.getBytes()));
            }
        }

        return result;
    }

    @Override
    @Attribute
    public String[] getUpdates() throws Exception {
        RequestContext ctx = getContext();
        final Timestamp oldTimestamp = ctx.lastTimestamp;
        final Timestamp timestamp = Timestamp.now();

        RequestContext updated = new RequestContext(ctx.useAliases, ctx.encode, ctx.outputType, timestamp, ctx.attributesGroup);
        setContext(updated);

        Multimap<AttributeName, AttributeValue<?>> attributes = engine.getAllAttributeValues(oldTimestamp, ctx.attributesGroup);

        AttributeValuesView view = new AttributeValuesView(attributes, ctx.useAliases);

        return processResult(view);
    }


    @Override
    @Attribute
    public String[] getMeta() {
        Iterable<Map.Entry<AttributeName, Class<?>>> data = engine.getAttributeClasses();

        return Iterables.toArray(Iterables.transform(data, new Function<Map.Entry<AttributeName, Class<?>>, String>() {
            @Override
            public String apply(Map.Entry<AttributeName, Class<?>> input) {
                return input.getKey().getFullName() + "->" + input.getValue().getName();
            }
        }), String.class);
    }

    //TODO commands
    @Override
    @Command
    @StateMachine(deniedStates = DeviceState.RUNNING)
    public void eraseData() {
        engine.clear();
    }

    @Override
    @Command
    @StateMachine(endState = DeviceState.RUNNING)
    public void startLightPolling() {
        stopCollectData();
        engine.startLightPolling();
        setStatus(Status.LIGHT_POLLING);
    }

    @Override
    @Command(inTypeDesc = "light polling rate in millis")
    @StateMachine(endState = DeviceState.RUNNING)
    public void startLightPollingAtFixedRate(long rate) {
        stopCollectData();
        engine.startLightPollingAtFixedRate(rate);
        setStatus(Status.LIGHT_POLLING_AT_FIXED_RATE);
    }

    @Override
    @Command
    @StateMachine(endState = DeviceState.RUNNING)
    public void startCollectData() {
        stopCollectData();
        engine.start();
        setStatus(Status.HEAVY_DUTY);
    }

    @Override
    @Command
    @StateMachine(endState = DeviceState.ON)
    public void stopCollectData() {
        engine.stop();
        setStatus(Status.IDLE);
    }

    @Override
    @Command
    public String[] getDataRange(long[] fromTo) throws Exception {
        Preconditions.checkArgument(fromTo.length == 2, "Two elements are expected here: 0 - from timestamp; 1 - to timestamp");
        Preconditions.checkArgument(Longs.compare(fromTo[0], fromTo[1]) < 0, "'from' should be less than 'to'!");
        Timestamp from = new Timestamp(fromTo[0]);
        Timestamp to = new Timestamp(fromTo[1]);

        RequestContext ctx = getContext();

        Multimap<AttributeName, AttributeValue<?>> values = engine.getValuesRange(from, to, ctx.attributesGroup);

        AttributeValuesView view = new AttributeValuesView(values, isUseAliases());

        return processResult(view);
    }

    @Override
    @Command
    public String[] getLatestSnapshot() throws Exception {
        RequestContext ctx = getContext();
        Multimap<AttributeName, AttributeValue<?>> values = engine.getLatestValues(ctx.attributesGroup);


        AttributeValuesView view = new AttributeValuesView(values, isUseAliases());

        return processResult(view);
    }

    @Override
    @Command
    public String[] getSnapshot(long value) throws Exception {
        Timestamp timestamp = new Timestamp(value);
        RequestContext ctx = getContext();
        Multimap<AttributeName, AttributeValue<?>> values = engine.getValues(timestamp, ctx.attributesGroup);

        AttributeValuesView view = new AttributeValuesView(values, isUseAliases());

        return processResult(view);
    }

    @Override
    @Command(inTypeDesc = "String array where first element is a group name and last elements are attribute full names.")
    public void createAttributesGroup(String[] args) throws Exception {
        RequestContext ctx = getContext();
        String cid = getClientId();
        String attributesGroup = args[0];
        RequestContext updated = new RequestContext(ctx.useAliases, ctx.encode, ctx.outputType, ctx.lastTimestamp, attributesGroup);
        setContext(updated);
        attributesGroupsMap.put(cid, attributesGroup);
        engine.createAttributesGroup(attributesGroup, Arrays.asList(Arrays.copyOfRange(args, 1, args.length)));
    }

    @Attribute
    public String[] getGroups() throws Exception {
        String cid = getClientId();
        return attributesGroupsMap.get(cid).toArray(new String[attributesGroupsMap.get(cid).size()]);
    }

    @Delete
    @StateMachine(endState = DeviceState.OFF)
    public void delete() {
        engine.shutdown();
    }

    public static void main(String... args) throws Exception {
        ServerManager.getInstance().start(args, StatusServer.class);
    }

    private RequestContext getContext() throws Exception {
        String cid = getClientId();
        RequestContext context = ctxs.get(cid);
        if (context == null) {
            ctxs.put(cid, context = new RequestContext());
            attributesGroupsMap.put(cid, AttributesManager.DEFAULT_ATTR_GROUP);
        }
        return context;
    }

    private void setContext(RequestContext context) throws Exception {
        String cid = getClientId();
        ctxs.put(cid, context);
    }

    @Attribute
    public String getClientId() throws Exception {
        ClntIdent clientIdentity = this.deviceManager.getClientIdentity();
        LockerLanguage discriminator = clientIdentity.discriminator();
        switch (discriminator.value()) {
            case LockerLanguage._JAVA:
                JavaClntIdent java_clnt = clientIdentity.java_clnt();
                return java_clnt.MainClass;
            case LockerLanguage._CPP:
                return "CPP " + clientIdentity.cpp_clnt();
        }
        throw new AssertionError("Should not happen");
    }

    private static enum OutputType {
        PLAIN,
        JSON
    }

    private static class RequestContext {
        private final boolean useAliases;
        private final boolean encode;
        private final OutputType outputType;
        private final Timestamp lastTimestamp;
        private final String attributesGroup;

        private RequestContext(boolean useAliases, boolean encode, OutputType outputType, Timestamp lastTimestamp, String attributesGroup) {
            this.useAliases = useAliases;
            this.encode = encode;
            this.outputType = outputType;
            this.lastTimestamp = lastTimestamp;
            this.attributesGroup = attributesGroup;
        }

        /**
         * Creates default context
         */
        private RequestContext() {
            this(false, false, OutputType.PLAIN, Timestamp.DEEP_PAST, AttributesManager.DEFAULT_ATTR_GROUP);
        }


    }
}
