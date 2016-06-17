package wpn.hdri.ss.tango;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.MoreExecutors;
import fr.esrf.Tango.AttrDataFormat;
import fr.esrf.Tango.AttrWriteType;
import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.PipeBlob;
import fr.esrf.TangoApi.PipeBlobBuilder;
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
import org.tango.server.annotation.*;
import org.tango.server.annotation.Attribute;
import org.tango.server.attribute.AttributeConfiguration;
import org.tango.server.attribute.AttributeValue;
import org.tango.server.attribute.IAttributeBehavior;
import org.tango.server.device.DeviceManager;
import org.tango.server.dynamic.DynamicManager;
import org.tango.server.pipe.PipeValue;
import org.tango.utils.ClientIDUtil;
import wpn.hdri.ss.configuration.StatusServerAttribute;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.configuration.StatusServerProperties;
import wpn.hdri.ss.data.Method;
import wpn.hdri.ss.data2.*;
import wpn.hdri.ss.engine2.Engine;
import wpn.hdri.ss.engine2.EngineFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.String;
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

    @Attribute
    public String[] getData(){
        Context ctx = contextManager.getContext();

        return recordsToStrings(engine.getStorage().getAllRecords().getRange(), ctx);
    }

    @Attribute
    public String[] getUpdates(){
        Context ctx = contextManager.getContext();

        long lastTimestamp = ctx.lastTimestamp;

        ctx.lastTimestamp = System.currentTimeMillis();

        return recordsToStrings(engine.getStorage().getAllRecords().getRange(lastTimestamp), ctx);
    }

    @Attribute
    public String getClientId() {
        return contextManager.getClientId();
    }

    @AroundInvoke
    public void aroundInvoke(InvocationContext invocationContext) {
        contextManager.setClientId(ClientIDUtil.toString(invocationContext.getClientID()));
    }

    //@StatusPipe
    @Pipe(name = "status")
    private PipeValue statusPipe;

    public PipeValue getStatusPipe(){
        PipeBlobBuilder pbb = new PipeBlobBuilder("status");

        //TODO monitored attributes
        //TODO last read value for an attribute (OK or FAILED)


        statusPipe.setValue(pbb.build(), System.currentTimeMillis());
        return statusPipe;
    }

    @Attribute
    public long getCrtTimestamp(){
        return System.currentTimeMillis();
    }

    @Pipe(name = "status_server_pipe")
    private PipeValue pipe;

    public void setPipe(PipeValue pipe){
        this.pipe = pipe;
    }

    public PipeValue getPipe(){
        Context ctx = contextManager.getContext();

        long lastTimestamp = ctx.lastTimestamp;
        ctx.lastTimestamp = System.currentTimeMillis();

        return new PipeValue(
                (PipeBlob) OutputType.PIPE.toType(
                        engine.getStorage().getAllRecords().getRange(lastTimestamp), false));
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
    public void dumpData(final String outputFilePath){
        final Context ctx = contextManager.getContext();
        final Path output = Paths.get(outputFilePath);
        if(Files.exists(output)) throw new IllegalArgumentException("Output file already exists! " + outputFilePath);
        exec.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String[] data = (String[]) OutputType.TSV.toType(engine.getStorage().getAllRecords().getRange(),ctx.useAliases);

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

        Context context = contextManager.getContext();

        return recordsToStrings(engine.getStorage().getAllRecords().getRange(t[0], t[1]), context);
    }

    private void checkRangeArguments(long[] t) {
        if(t.length != 2) throw new IllegalArgumentException("Exactly two arguments are expected here: t0&t1");
        if(t[0] >= t[1]) throw new IllegalArgumentException("t0 must be LT t1");
    }

    @Command
    public String[] getDataRangeInterpolated(long[] t){
        checkRangeArguments(t);

        final Context context = contextManager.getContext();

        Iterable<? extends Snapshot> snapshots = engine.getStorage().getAllRecords().getSnapshots(t[0], t[1]);

        final int dataSize = Iterables.size(snapshots);


        int groupSize = context.attributesGroup.size();//TODO fix NPE
        List<SingleRecord<?>> result = new ArrayList<>();


        //initialize input data array
        InterpolationInputData[] inputDatas = new InterpolationInputData[groupSize];

        for(int i = 0; i<groupSize ; ++i){
            inputDatas[i] = new InterpolationInputData(dataSize);
        }

        for (int ndx = 0; ndx<dataSize; ++ndx) {
            Snapshot snapshot = Iterables.get(snapshots, ndx);
            Iterable<SingleRecord<?>> filtered = filteredRecords(snapshot, context);

            for (int i = 0; i < groupSize; ++i) {
                inputDatas[i].x[ndx] = (double) Iterables.get(filtered, i).r_t;
                inputDatas[i].x[ndx] = (double) (Double)(Object)Iterables.get(filtered, i).value;
            }
        }

        LinearInterpolator interpolator = new LinearInterpolator();

        for(int i = 0;i <groupSize; ++i){
            long v = t[0] + ((t[1] - t[0]) / 2);
            result.add(new SingleRecord<>(null,v,v,interpolator.interpolate(inputDatas[0].x, inputDatas[0].y).value(v)));
        }


        return recordsToStrings(result, context);
    }

    private class InterpolationInputData {
        final double[] x;
        final double[] y;

        public InterpolationInputData(int size) {
            this.x = new double[size];
            this.y = new double[size];
        }
    }

    @Command
    public String[] getLatestSnapshot() {
        Context ctx = contextManager.getContext();

        return recordsToStrings(engine.getStorage().getSnapshot(), ctx);
    }

    @Command
    public String[] getSnapshot(long t){
        Context context = contextManager.getContext();

        return recordsToStrings(engine.getStorage().getAllRecords().getRange(t), context);
    }

    @Command(name="startCollectData")
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

    @Command(name="stopCollectData")
    @StateMachine(endState = DeviceState.ON)
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


    public static void main(String[] args) {
        ServerManager.getInstance().start(args, StatusServer2.class);
    }


    private static Iterable<SingleRecord<?>> filteredRecords(Iterable<SingleRecord<?>> snapshot, final Context ctx){
        return ctx.attributesGroup.isDefault() ?
                snapshot :
                Iterables.filter(snapshot, new Predicate<SingleRecord<?>>() {
                    @Override
                    public boolean apply(SingleRecord<?> input) {
                        if (input == null) return false;
                        return ctx.attributesGroup.hasAttribute(input.attribute);
                    }
                });
    }

        private static String[] recordsToStrings(Iterable<SingleRecord<?>> snapshot, final Context ctx) {
            Iterable<SingleRecord<?>> filtered = filteredRecords(snapshot, ctx);
            return (String[]) ctx.outputType.toType(filtered, ctx.useAliases);
    }
}
