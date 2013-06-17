package wpn.hdri.ss.tango;

//import org.tango.DeviceState;
import org.tango.DeviceState;
import org.tango.server.ServerManager;
import org.tango.server.annotation.*;
import wpn.hdri.ss.engine.Engine;

import java.util.concurrent.atomic.AtomicLong;

/**
 * StatusServer Tango implementation based on the new JTangoServer library
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 17.06.13
 */
@Device
public class JStatusServer {
    private final AtomicLong clientId = new AtomicLong(1);

    private Engine engine;

    @State
    private DeviceState state = DeviceState.OFF;

    @Init
    public void init() throws Exception{
        //TODO init engine

        setState(DeviceState.ON);
    }

    //TODO attributes
    @Attribute
    private boolean isUseAliases = false;

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
    private String dataEncoded;

    //TODO commands
    @Command
    public void eraseData(){
        engine.clear();
    }

    @Command
    public void startLightPolling(){
        engine.startLightPolling();
        setState(DeviceState.RUNNING);
    }

    @Command(inTypeDesc = "light polling rate in millis")
    public void startLightPollingAtFixedRate(long rate){
        engine.startLightPollingAtFixedRate(rate);
        setState(DeviceState.RUNNING);
    }

    @Command
    public void startCollectData(){
        engine.start();
        setState(DeviceState.RUNNING);
    }

    @Command
    public void stopCollectData(){
        engine.stop();
        setState(DeviceState.ON);
    }

    @Command(outTypeDesc = "client id is used in getXXXUpdates methods")
    public long registerClient(){
        return clientId.getAndIncrement();
    }

    @Command
    public String[] getDataUpdates(long clientId){
        return new String[0];
    }

    //TODO transform to Attribute
    @Command
    public String[] getData(){
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
    public void delete(){
        engine.shutdown();
    }

    public static void main(String... args) throws Exception{
        ServerManager.getInstance().start(args, JStatusServer.class);
    }

    public DeviceState getState() {
        return state;
    }

    public void setState(DeviceState state) {
        this.state = state;
    }
}
