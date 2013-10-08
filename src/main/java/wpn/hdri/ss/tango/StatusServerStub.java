package wpn.hdri.ss.tango;

import org.tango.DeviceState;
import org.tango.server.annotation.Attribute;
import org.tango.server.annotation.Command;
import org.tango.server.annotation.Init;
import org.tango.server.annotation.StateMachine;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 20.06.13
 */
public interface StatusServerStub {
    DeviceState getState();

    String getStatus();

    @Init
    @StateMachine(endState = DeviceState.ON)
    void init() throws Exception;

//    @Attribute
//    @AttributeProperties(description = "clientId is used in getXXXUpdates methods as an argument.")
//    int getClientId();

    String[] getMeta();

    void setUseAliases(boolean v) throws Exception;

    @Attribute
    long getCrtTimestamp();

    @Command
    String[] getUpdates() throws Exception;

    @Attribute
    String[] getData() throws Exception;

    //TODO commands
    @Command
    void eraseData();

    @Command
    @StateMachine(endState = DeviceState.RUNNING)
    void startLightPolling();

    @Command(inTypeDesc = "light polling rate in millis")
    @StateMachine(endState = DeviceState.RUNNING)
    void startLightPollingAtFixedRate(long rate);

    @Command
    @StateMachine(endState = DeviceState.RUNNING)
    void startCollectData();

    @Command
    @StateMachine(endState = DeviceState.ON)
    void stopCollectData();

    @Command
    String[] getDataRange(long[] fromTo) throws Exception;

    @Command
    String[] getLatestSnapshot() throws Exception;

    @Command
    String[] getSnapshot(long value) throws Exception;

    @Command(inTypeDesc = "String array where first element is a group name and last elements are attribute full names.")
    void createAttributesGroup(String[] args) throws Exception;
}
