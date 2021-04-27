package wpn.hdri.ss.tango;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Doubles;
import com.google.common.util.concurrent.MoreExecutors;
import fr.esrf.Tango.AttrDataFormat;
import fr.esrf.Tango.AttrWriteType;
import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.PipeBlob;
import hzg.wpn.xenv.ResourceManager;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tango.DeviceState;
import org.tango.client.ez.data.type.TangoDataType;
import org.tango.client.ez.data.type.TangoDataTypes;
import org.tango.client.ez.data.type.UnknownTangoDataType;
import org.tango.server.InvocationContext;
import org.tango.server.ServerManager;
import org.tango.server.StateMachineBehavior;
import org.tango.server.annotation.Attribute;
import org.tango.server.annotation.*;
import org.tango.server.attribute.AttributeConfiguration;
import org.tango.server.attribute.AttributeValue;
import org.tango.server.attribute.IAttributeBehavior;
import org.tango.server.device.DeviceManager;
import org.tango.server.dynamic.DynamicManager;
import org.tango.server.pipe.PipeValue;
import org.tango.utils.ClientIDUtil;
import wpn.hdri.ss.configuration.StatusServerAttribute;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.data.Method;
import wpn.hdri.ss.data2.*;
import wpn.hdri.ss.engine2.Engine;
import wpn.hdri.ss.engine2.EngineFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 11.11.2015
 */
@Device(deviceType = "xenv.StatusServer", transactionType = TransactionType.NONE)
public class StatusServer2 {
    private static final Logger logger = LoggerFactory.getLogger(StatusServer2.class);

    private final ExecutorService exec = MoreExecutors.getExitingExecutorService((ThreadPoolExecutor) Executors.newFixedThreadPool(1), 3L, TimeUnit.SECONDS);

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

    @State(isPolled = true, pollingPeriod = 3000)
    private DeviceState state;

    public DeviceState getState() {
        return state;
    }

    @Status(isPolled = true, pollingPeriod = 3000)
    private String status;

    public void setState(DeviceState state) {
        this.state = state;
    }


    public String getStatus() {
        return status;
    }

    public static void main(String[] args) {
        ServerManager.getInstance().start(args, StatusServer2.class);
    }


    private Engine engine;

    private ContextManager contextManager;;

    @Attribute
    public boolean getUseAliases() {
        return contextManager.getContext().useAliases;
    }

    //TODO move to StatusPipe
    @Attribute
    public String[] getFailedToInitializeAttributes() {
        return Iterables.toArray(contextManager.getFailedAttributes(), String.class);
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
        return contextManager.getGroupName();
    }

    @Attribute
    public void setGroup(String attributesGroup) {
        contextManager.selectGroup(attributesGroup);
    }

    @Attribute
    public String[] getGroups(){
        return Iterables.toArray(contextManager.getGroups(), String.class);
    }

    //TODO data obtaining code must be encapsulated into a dedicated class, which will handle current status and context
    @Attribute
    public String[] getData(){
        Context ctx = contextManager.getContext();

        Iterable<SingleRecord<?>> range;
        if(StatusServerStatus.HEAVY_DUTY == getStatus())
            range = engine.getStorage().getAllRecords().getRange();
        else
            range = engine.getStorage().getSnapshot();
        FilteredRecords filteredRange = new FilteredRecords(contextManager.getGroup(), range);


        return recordsToStrings(filteredRange, ctx);
    }

    @Attribute
    public String[] getUpdates(){
        Context ctx = contextManager.getContext();

        long lastTimestamp = ctx.lastTimestamp;

        ctx.lastTimestamp = System.currentTimeMillis();

        Iterable<SingleRecord<?>> range;
        if(StatusServerStatus.HEAVY_DUTY == getStatus())
            range = engine.getStorage().getAllRecords().getRange(lastTimestamp);
        else
            range = engine.getStorage().getSnapshot();
        FilteredRecords filteredRange = new FilteredRecords(contextManager.getGroup(), range);
        return recordsToStrings(filteredRange, ctx);
    }

    @Attribute
    public String getClientId() {
        return contextManager.getClientId();
    }

    @AroundInvoke
    public void aroundInvoke(InvocationContext invocationContext) {
        Optional.ofNullable(contextManager)
                .orElseGet(() -> new ContextManager(Collections.emptyList(), Collections.emptyList()))
                .setClientId(ClientIDUtil.toString(invocationContext.getClientID()));
    }

    @Attribute
    public long getCrtTimestamp() {
        return System.currentTimeMillis();
    }

    @Attribute
    public long getMaintenanceDelay() {
        return this.engine.getMaintenanceDelay();
    }

    @Attribute
    @StateMachine(deniedStates = DeviceState.RUNNING)
    public void setMaintenanceDelay(long newDelay) {
        this.engine.setMaintenanceDelay(newDelay);
    }

    @Pipe(name = "status_server_pipe")
    private PipeValue pipe;

    public void setPipe(PipeValue pipe) {
        this.pipe = pipe;
    }

    public PipeValue getPipe() {
        Context ctx = contextManager.getContext();

        long lastTimestamp = ctx.lastTimestamp;
        ctx.lastTimestamp = System.currentTimeMillis();

        PipeValue value = new PipeValue(
                (PipeBlob) OutputType.PIPE.toType(
                        engine.getStorage().getAllRecords().getRange(lastTimestamp), ctx));
        return value;
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

        contextManager.setGroup(new AttributesGroup(groupName, attributes));
    }

    @Command
    public void dumpData(final String outputFilePath){
        final Context ctx = contextManager.getContext();
        final Path output = Paths.get(outputFilePath);
        if(Files.exists(output)) throw new IllegalArgumentException("Output file already exists! " + outputFilePath);
        exec.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String[] data = (String[]) OutputType.TSV.toType(engine.getStorage().getAllRecords().getRange(),ctx);

                    Files.write(output, Arrays.asList("Name or Alias\tRead@\tValue\tWritten@\n"), Charset.defaultCharset(), StandardOpenOption.CREATE_NEW);
                    Files.write(output, Arrays.asList(data), Charset.defaultCharset(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
    }

    @Command
    @StateMachine(deniedStates = DeviceState.RUNNING)
    public void eraseData(){
        engine.getStorage().clear();
    }

    @Command
    public String[] getDataRange(long[] t){
        checkRangeArguments(t);

        Context ctx = contextManager.getContext();

        Iterable<SingleRecord<?>> range = engine.getStorage().getAllRecords().getRange(t[0], t[1]);
        FilteredRecords filteredRange = new FilteredRecords(contextManager.getGroup(), range);
        return recordsToStrings(filteredRange, ctx);
    }

    private void checkRangeArguments(long[] t) {
        if(t.length != 2) throw new IllegalArgumentException("Exactly two arguments are expected here: t0&t1");
        if(t[0] >= t[1]) throw new IllegalArgumentException("t0 must be LT t1");
    }

    @Command
    public String[] getDataRangeInterpolated(long[] t){
        checkRangeArguments(t);

        final Context context = contextManager.getContext();

        Iterable<SingleRecord<?>> range = engine.getStorage().getAllRecords().getRange(t[0], t[1]);
        FilteredRecords records = new FilteredRecords(contextManager.getGroup(), range);


        Map<String, InterpolationInputData> inputDataMap = Maps.newHashMap();


        for(SingleRecord<?> record : records){
            String attributeFullName = record.attribute.fullName;//TODO possibly slow read from memory

            InterpolationInputData inputData = inputDataMap.getOrDefault(attributeFullName, new InterpolationInputData(record.attribute));

            inputData.add((SingleRecord<Object>) record);//TODO avoid autoboxing

            inputDataMap.putIfAbsent(attributeFullName, inputData);
        }

        List<SingleRecord<?>> result = new ArrayList<>();
        LinearInterpolator interpolator = new LinearInterpolator();

        long v = t[0] + ((t[1] - t[0]) / 2);
        for(InterpolationInputData inputData : inputDataMap.values()){
            if(inputData.size() == 1){
                result.add(inputData.records.get(0));
            } else {
                result.add(
                        new SingleRecord<>(
                                (wpn.hdri.ss.data2.Attribute<Object>) inputData.attribute,
                                v, v,
                                interpolator.interpolate(inputData.x(), inputData.y()).value(v)));
            }
        }

        return recordsToStrings(result, context);//TODO use decoration
    }

    private class InterpolationInputData {
        final wpn.hdri.ss.data2.Attribute<?> attribute;

        List<SingleRecord<?>> records = new ArrayList<>();

        private InterpolationInputData(wpn.hdri.ss.data2.Attribute<?> attribute) {
            this.attribute = attribute;
        }

        void add(SingleRecord<Object> record){
            this.records.add(record);
        }

        double[] x(){
            return Doubles.toArray(Lists.transform(records, new Function<SingleRecord<?>, Number>() {
                @Nullable
                @Override
                public Number apply(@Nullable SingleRecord<?> input) {
                    return Long.valueOf(input.r_t);
                }
            }));
        }

        double[] y(){
            return Doubles.toArray(Lists.transform(records, new Function<SingleRecord<?>, Number>() {
                @Nullable
                @Override
                public Number apply(@Nullable SingleRecord<?> input) {
                    return (Number) input.value;
                }
            }));
        }

        int size(){
            return records.size();
        }
    }

    @Command
    public String[] getLatestSnapshot() {
        Context ctx = contextManager.getContext();

        Snapshot snapshot = engine.getStorage().getSnapshot();
        FilteredSnapshot filteredSnapshot = new FilteredSnapshot(contextManager.getGroup(), snapshot);

        return recordsToStrings(filteredSnapshot, ctx);
    }

    @Command
    public String[] getSnapshot(long t){
        Context ctx = contextManager.getContext();

        Iterable<SingleRecord<?>> range = engine.getStorage().getAllRecords().getRange(t);
        FilteredRecords filteredRange = new FilteredRecords(contextManager.getGroup(), range);
        return recordsToStrings(filteredRange, ctx);
    }

    public void setStatus(String status) {
        this.status = String.format("%d: %s", System.currentTimeMillis(), status);
    }

    @Command(name="startCollectData")
    @StateMachine(deniedStates = {DeviceState.RUNNING})
    public void start() {
        engine.start();
        deviceManager.pushStateChangeEvent(DeviceState.RUNNING);
        deviceManager.pushStatusChangeEvent(StatusServerStatus.HEAVY_DUTY);
    }

    @Command
    @StateMachine(endState = DeviceState.RUNNING, deniedStates = {DeviceState.RUNNING})
    public void startLightPoling() {
        engine.startLightPolling();
        deviceManager.pushStateChangeEvent(DeviceState.RUNNING);
        deviceManager.pushStatusChangeEvent(StatusServerStatus.LIGHT_POLLING);
    }

    @Command
    @StateMachine(deniedStates = {DeviceState.RUNNING})
    public void startLightPolingAtFixedRate(long delay) {
        engine.startLightPollingAtFixedRate(delay);
        deviceManager.pushStateChangeEvent(DeviceState.RUNNING);
        deviceManager.pushStatusChangeEvent(StatusServerStatus.LIGHT_POLLING_AT_FIXED_RATE);
    }

    @Command(name="stopCollectData")
    public void stop() {
        engine.stop();
        deviceManager.pushStateChangeEvent(DeviceState.ON);
        deviceManager.pushStatusChangeEvent(StatusServerStatus.IDLE);
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
                type = dataType.getDataTypeClass();
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

    @Init
    @StateMachine(deniedStates = DeviceState.RUNNING)
    public void init() throws Exception {
        String devName = deviceManager.getName().split("/")[2];

        InputStream xmlStream = ResourceManager.loadResource("etc/StatusServer", devName + ".xml");

        logger.info("Loading configuration...");
        StatusServerConfiguration configuration = StatusServerConfiguration.fromXmlStream(xmlStream);
        logger.info("Done.");

        List<wpn.hdri.ss.data2.Attribute<?>> selfAttributes = initializeStatusServerAttributes(configuration, dynamicManager);

        EngineFactory engineFactory = new EngineFactory(selfAttributes, configuration);
        this.engine = engineFactory.newEngine();

        this.contextManager = new ContextManager(engine.getAttributes(), engineFactory.getFailedAttributes());

        deviceManager.pushStateChangeEvent(DeviceState.ON);

        if (!engineFactory.getFailedAttributes().isEmpty()) {
            deviceManager.pushStatusChangeEvent("Some attributes in configuration has failed to initialize!");
        } else {
            deviceManager.pushStatusChangeEvent(StatusServerStatus.IDLE);
        }
    }

    //TODO the following must be refactored as decorators
    private static String[] recordsToStrings(Iterable<SingleRecord<?>> snapshot, final Context ctx) {
        return (String[]) ctx.outputType.toType(snapshot, ctx);
    }
}
