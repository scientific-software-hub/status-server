package wpn.hdri.ss.tango;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import hzg.wpn.properties.PropertiesParser;
import org.tango.DeviceState;
import org.tango.server.ServerManager;
import org.tango.server.annotation.*;
import wpn.hdri.ss.StatusServerProperties;
import wpn.hdri.ss.configuration.ConfigurationBuilder;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.engine.Engine;
import wpn.hdri.ss.engine.EngineInitializer;

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
    @Attribute
    @AttributeProperties(description = "clientId is used in getXXXUpdates methods as an argument.")
    private final AtomicLong clientId = new AtomicLong(0);

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
    private String crtActivity;

    public String getCrtActivity(){
        return engine.getCurrentActivity();
    }

    @Attribute
    private long crtTimestamp;

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
        engine.startLightPolling();
        setStatus(Status.LIGHT_POLLING);
    }

    @Command(inTypeDesc = "light polling rate in millis")
    @StateMachine(endState = DeviceState.RUNNING)
    public void startLightPollingAtFixedRate(long rate){
        engine.startLightPollingAtFixedRate(rate);
        setStatus(Status.LIGHT_POLLING_AT_FIXED_RATE);
    }

    @Command
    @StateMachine(endState = DeviceState.RUNNING)
    public void startCollectData(){
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
        return new String[0];
    }

    @Command
    public String[] getLatestSnapshotByGroup(){
        return new String[0];
    }

    @Command
    public String[] getSnapshot(long timestamp){
        return new String[0];
    }

    @Command
    public String[] getSnapshotByGroup(long timestamp){
        return new String[0];
    }

    @Command
    public void createAttributesGroup(String[] args){

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
