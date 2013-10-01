package wpn.hdri.ss.tango;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import fr.esrf.Tango.AttrWriteType;
import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.LockerLanguage;
import hzg.wpn.properties.PropertiesParser;
import hzg.wpn.util.compressor.Compressor;
import org.tango.DeviceState;
import org.tango.server.ServerManager;
import org.tango.server.StateMachineBehavior;
import org.tango.server.annotation.*;
import org.tango.server.attribute.AttributeConfiguration;
import org.tango.server.attribute.IAttributeBehavior;
import org.tango.server.dynamic.DynamicManager;
import wpn.hdri.ss.StatusServerProperties;
import wpn.hdri.ss.configuration.ConfigurationBuilder;
import wpn.hdri.ss.configuration.StatusServerAttribute;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.data.attribute.AttributeName;
import wpn.hdri.ss.data.attribute.AttributeValue;
import wpn.hdri.ss.data.attribute.AttributeValuesView;
import wpn.hdri.ss.engine.*;
import wpn.hdri.tango.data.type.ScalarTangoDataTypes;
import wpn.hdri.tango.data.type.TangoDataType;
import wpn.hdri.tango.data.type.TangoDataTypes;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * StatusServer Tango implementation based on the new JTangoServer library
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 17.06.13
 */
@Device
public class StatusServer implements StatusServerStub {
    private static String XML_CONFIG_PATH;
    private final static String DEFAULT_ATTR_GROUP = "default";
    private final Multimap <String, String> attributesGroupsMap = HashMultimap.create();
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
    // ====================

    @Override
    @Init
    @StateMachine(endState = DeviceState.ON)
    public void init() throws Exception {
        Preconditions.checkNotNull(XML_CONFIG_PATH, "Path to xml configuration is not set.");
        StatusServerConfiguration configuration = new ConfigurationBuilder().fromXml(XML_CONFIG_PATH);

        StatusServerProperties properties = PropertiesParser.createInstance(StatusServerProperties.class).parseProperties();

        EngineInitializer initializer = new EngineInitializer(configuration, properties);

        initializeDynamicAttributes(configuration.getStatusServerAttributes(), dynamicManagement);

        EngineInitializationContext ctx = initializer.initialize();

        this.engine = new Engine(ctx);
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
                    TangoDataType<?> dataType = TangoDataTypes.forString(wrapped.getType());
//                    configuration.setTangoType(dataType.getAlias(), AttrDataFormat.FMT_UNKNOWN);
                    configuration.setType(dataType.getDataType());
                    configuration.setWritable(AttrWriteType.READ_WRITE);
                    return configuration;
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
    public void setAttributesGroup(String attributesGroup) throws Exception {
        RequestContext ctx = getContext();
        RequestContext updated = new RequestContext(ctx.useAliases, ctx.encode, ctx.outputType, ctx.lastTimestamp, attributesGroup);
        setContext(updated);
    }
    @Attribute
    public String getAttributesGroup() throws Exception {
        RequestContext cxt = getContext();
        return cxt.attributesGroup;
    }

    public AttributeFilter getFilter() throws Exception {
          if (getAttributesGroup() == DEFAULT_ATTR_GROUP){
              return AttributeFilters.none();
          }
        return AttributeFilters.byGroup(getAttributesGroup());
    }


    //TODO attributes
    @Attribute
    @Override
    public void setUseAliases(boolean v) throws Exception{
        String cid = getClientId();
        RequestContext old = getContext();
        RequestContext ctx = new RequestContext(v,old.encode,old.outputType,old.lastTimestamp, old.attributesGroup);
        ctxs.put(cid,ctx);
    }

    private boolean isUseAliases() throws Exception{
        return getContext().useAliases;
    }

    @Override
    @Attribute
    public long getCrtTimestamp() {
        return System.currentTimeMillis();
    }

    @Override
    @Attribute
    public String[] getData() throws Exception{
        RequestContext ctx = getContext();
        Multimap<AttributeName, AttributeValue<?>> attributes = engine.getAllAttributeValues(null, getFilter());

        AttributeValuesView view = new AttributeValuesView(attributes, ctx.useAliases);
        return processResult(view);
    }

    private String[] processResult(AttributeValuesView view) throws Exception {
        RequestContext ctx = getContext();
        String[] result = new String[0];
        switch (ctx.outputType){
            case PLAIN:
                result = view.toStringArray();
                break;
            case JSON:
                result = new String[]{view.toJsonString()};
                break;
        }
        if(ctx.encode){
            for (int i = 0, resultLength = result.length; i < resultLength; i++) {
                String string = result[i];

                result[i] = new String(Compressor.encodeAndCompress(string.getBytes()));
            }
        }

        return result;
    }

    @Override
    @Attribute
    public String[] getUpdates() throws Exception{
        RequestContext ctx = getContext();
        final Timestamp oldTimestamp = ctx.lastTimestamp;
        final Timestamp timestamp = Timestamp.now();

        RequestContext updated = new RequestContext(ctx.useAliases, ctx.encode, ctx.outputType, timestamp, ctx.attributesGroup);
        setContext(updated);

        Multimap<AttributeName, AttributeValue<?>> attributes = engine.getAllAttributeValues(oldTimestamp, getFilter());

        AttributeValuesView view = new AttributeValuesView(attributes, ctx.useAliases);

        return processResult(view);
    }


    @Override
    @Attribute
    public String[] getMeta() {
        Iterable<Map.Entry<AttributeName, Class<?>>> data = engine.getAttributeClasses();

        String[] result = new String[Iterables.size(data)];
        int i = 0;
        for (Map.Entry<AttributeName, Class<?>> entry : data) {
            TangoDataType<?> dataType = TangoDataTypes.forClass(entry.getValue());
            //TODO TINE has completely different types, i.e. NAME64
            if (dataType == null)
                dataType = ScalarTangoDataTypes.STRING;
            result[i++] = entry.getKey().getFullName() + "->" + dataType.toString();
        }

        return result;
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
    public String[] getLatestSnapshot() throws Exception{
        Multimap<AttributeName, AttributeValue<?>> values = engine.getLatestValues(getFilter());

        AttributeValuesView view = new AttributeValuesView(values, isUseAliases());

        String[] output = view.toStringArray();
        return output;
    }

    @Override
    @Command
    public String[] getSnapshot(long value) throws Exception{
        Timestamp timestamp = new Timestamp(value);
        Multimap<AttributeName, AttributeValue<?>> values = engine.getValues(timestamp, getFilter());

        AttributeValuesView view = new AttributeValuesView(values, isUseAliases());

        String[] output = view.toStringArray();
        return output;
    }

    @Override
    @Command(inTypeDesc = "String array where first element is a group name and last elements are attribute full names.")
    public void createAttributesGroup(String[] args) throws Exception {
        RequestContext ctx = getContext();
        String cid = getClientId();
        String attributesGroup = args[0];
        RequestContext updated = new RequestContext(ctx.useAliases, ctx.encode, ctx.outputType, ctx.lastTimestamp, attributesGroup);
        setContext(updated);
        attributesGroupsMap.put(cid,attributesGroup);
        engine.createAttributesGroup(attributesGroup, Arrays.asList(Arrays.copyOfRange(args, 1, args.length)));
    }

    @Attribute
    public String[] getAttributesGroupsMap() throws Exception {
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

    private RequestContext getContext() throws Exception{
        String cid = getClientId();
        RequestContext context = ctxs.get(cid);
        if(context == null) {
            ctxs.put(cid, context = new RequestContext());
            attributesGroupsMap.put(cid,DEFAULT_ATTR_GROUP);
        }
        return context;
    }

    private void setContext(RequestContext context) throws Exception{
        String cid = getClientId();
        ctxs.put(cid,context);
    }

    //TODO avoid this dirty hack
    private String getClientId() throws Exception{
        Field deviceImpl = this.dynamicManagement.getClass().getDeclaredField("deviceImpl");
        deviceImpl.setAccessible(true);
        Field clientIdentity = deviceImpl.get(this.dynamicManagement).getClass().getDeclaredField("clientIdentity");
        clientIdentity.setAccessible(true);
        Field discriminator = clientIdentity.get(deviceImpl.get(this.dynamicManagement)).getClass().getDeclaredField("discriminator");
        discriminator.setAccessible(true);
        LockerLanguage value = (LockerLanguage) discriminator.get(clientIdentity.get(deviceImpl.get(this.dynamicManagement)));
        switch (value.value()){
            case LockerLanguage._JAVA:
                Field java_clnt = clientIdentity.get(deviceImpl.get(this.dynamicManagement)).getClass().getDeclaredField("java_clnt");
                java_clnt.setAccessible(true);
                Field mainClass = java_clnt.get(clientIdentity.get(deviceImpl.get(this.dynamicManagement))).getClass().getDeclaredField("MainClass");
                mainClass.setAccessible(true);
                return mainClass.get(java_clnt.get(clientIdentity.get(deviceImpl.get(this.dynamicManagement)))).toString();
            case LockerLanguage._CPP:
                //TODO
                return null;
        }
        throw new AssertionError("Should not happen");
    }

    private static enum OutputType{
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
            this(false, false, OutputType.PLAIN, Timestamp.DEEP_PAST, DEFAULT_ATTR_GROUP);
        }


    }
}
