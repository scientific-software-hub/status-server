package wpn.hdri.ss.tango;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import hzg.wpn.properties.PropertiesParser;
import org.tango.DeviceState;
import org.tango.server.ServerManager;
import org.tango.server.annotation.*;
import wpn.hdri.ss.StatusServerProperties;
import wpn.hdri.ss.configuration.ConfigurationBuilder;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.data.AttributeName;
import wpn.hdri.ss.data.AttributeValue;
import wpn.hdri.ss.data.AttributesView;
import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.engine.AttributeFilters;
import wpn.hdri.ss.engine.Engine;
import wpn.hdri.ss.engine.EngineInitializer;

import java.util.Arrays;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * StatusServer Tango implementation based on the new JTangoServer library
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 17.06.13
 */
@Device
public class JStatusServer {
    private static String XML_CONFIG_PATH;

    public static void setXmlConfigPath(String v){
        XML_CONFIG_PATH = v;
    }

    private static interface Status {
        String IDLE = "IDLE";
        String LIGHT_POLLING = "LIGHT_POLLING";
        String LIGHT_POLLING_AT_FIXED_RATE = "LIGHT_POLLING_AT_FIXED_RATE";
        String HEAVY_DUTY = "HEAVY_DUTY";
    }

    /**
     * This field tracks timestamps of the clients and is used in getXXXUpdates methods
     */
    private final ConcurrentMap<Long,Timestamp> timestamps = Maps.newConcurrentMap();


    private Engine engine;

    public JStatusServer() {
        System.out.println("Create instance");
    }

    @State
    private DeviceState state = DeviceState.OFF;

    public DeviceState getState() {
        return state;
    }

    public void setState(DeviceState state) {
        this.state = state;
    }

    @org.tango.server.annotation.Status
    private String status = Status.IDLE;

    public void setStatus(String v){
        this.status = v;
    }

    public String getStatus(){
        return this.status;
    }

    @Init
    @StateMachine(endState = DeviceState.ON)
    public void init() throws Exception {
        Preconditions.checkNotNull(XML_CONFIG_PATH,"Path to xml configuration is not set.");
        StatusServerConfiguration configuration = new ConfigurationBuilder().fromXml(XML_CONFIG_PATH);

        StatusServerProperties properties = new PropertiesParser<StatusServerProperties>(StatusServerProperties.class).parseProperties();

        EngineInitializer initializer = new EngineInitializer(configuration,properties);

        this.engine = initializer.initialize();
    }

    //TODO attributes
    private final AtomicLong clientId = new AtomicLong(0);
    @Attribute
    @AttributeProperties(description = "clientId is used in getXXXUpdates methods as an argument.")
    public long getClientId(){
        return clientId.incrementAndGet();
    }

    //TODO make useAliases client specific
    @Attribute
    private boolean useAliases = false;

    public void setUseAliases(boolean v){
        this.useAliases = v;
    }

    public boolean isUseAliases(){
        return this.useAliases;
    }

    @Attribute
    public String getCrtActivity(){
        return engine.getCurrentActivity();
    }

    @Attribute
    public long getCrtTimestamp(){
        return System.currentTimeMillis();
    }

    @Attribute
    public String[] getData(){
        //TODO
        return new String[0];
    }

    @Attribute
    public String getDataEncoded(){
        //TODO
        return "";
    }

    //TODO commands
    @Command
    public void eraseData(){
        engine.clear();
    }

    @Command
    @StateMachine(endState = DeviceState.RUNNING)
    public void startLightPolling(){
        stopCollectData();
        engine.startLightPolling();
        setStatus(Status.LIGHT_POLLING);
    }

    @Command(inTypeDesc = "light polling rate in millis")
    @StateMachine(endState = DeviceState.RUNNING)
    public void startLightPollingAtFixedRate(long rate){
        stopCollectData();
        engine.startLightPollingAtFixedRate(rate);
        setStatus(Status.LIGHT_POLLING_AT_FIXED_RATE);
    }

    @Command
    @StateMachine(endState = DeviceState.RUNNING)
    public void startCollectData(){
        stopCollectData();
        engine.start();
        setStatus(Status.HEAVY_DUTY);
    }

    @Command
    @StateMachine(endState = DeviceState.ON)
    public void stopCollectData(){
        engine.stop();
        setStatus(Status.IDLE);
    }

    @Command
    public String[] getDataUpdates(long clientId){
        return new String[0];
    }

    @Command
    public String getDataUpdatesEncoded(){
        return "";
    }

    @Command
    public String[] getLatestSnapshot(){
        Multimap<AttributeName, AttributeValue<?>> values = engine.getLatestValues(AttributeFilters.none());

        AttributesView view = new AttributesView(values, isUseAliases());

        String[] output = view.toStringArray();
        return output;
    }

    @Command
    public String[] getLatestSnapshotByGroup(String groupName){
        Multimap<AttributeName, AttributeValue<?>> values = engine.getLatestValues(AttributeFilters.byGroup(groupName));

        AttributesView view = new AttributesView(values, isUseAliases());

        String[] output = view.toStringArray();
        return output;
    }

    @Command
    public String[] getSnapshot(long value){
        Timestamp timestamp = new Timestamp(value);
        Multimap<AttributeName, AttributeValue<?>> values = engine.getValues(timestamp, AttributeFilters.none());

        AttributesView view = new AttributesView(values, isUseAliases());

        String[] output = view.toStringArray();
        return output;
    }

    @Command
    public String[] getSnapshotByGroup(String[] data_in){
        long value = Long.parseLong(data_in[0]);
        String groupName = data_in[1];

        Timestamp timestamp = new Timestamp(value);
        Multimap<AttributeName, AttributeValue<?>> values = engine.getValues(timestamp, AttributeFilters.byGroup(groupName));

        AttributesView view = new AttributesView(values, isUseAliases());

        String[] output = view.toStringArray();
        return output;
    }

    @Command(inTypeDesc = "String array where first element is a group name and last elements are attribute full names.")
    public void createAttributesGroup(String[] args){
        engine.createAttributesGroup(args[0], Arrays.asList(Arrays.copyOfRange(args, 1, args.length)));
    }

    @Delete
    @StateMachine(endState = DeviceState.OFF)
    public void delete(){
        engine.shutdown();
    }

    public static void main(String... args) throws Exception{
        ServerManager.getInstance().start(args, JStatusServer.class);
    }
}
