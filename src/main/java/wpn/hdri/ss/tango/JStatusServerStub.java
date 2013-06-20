package wpn.hdri.ss.tango;

import org.tango.DeviceState;
import org.tango.server.annotation.*;

import java.io.IOException;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 20.06.13
 */
public interface JStatusServerStub {
    DeviceState getState();

    String getStatus();

    @Init
    @StateMachine(endState = DeviceState.ON)
    void init() throws Exception;

    @Attribute
    @AttributeProperties(description = "clientId is used in getXXXUpdates methods as an argument.")
    int getClientId();

    void setUseAliases(boolean v);

    boolean isUseAliases();

    @Attribute
    String getCrtActivity();

    @Attribute
    long getCrtTimestamp();

    @Attribute
    String[] getData();

    @Attribute
    String getDataEncoded() throws IOException;

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
    String[] getUpdates(int clientId);

    @Command
    String getUpdatesEncoded(int clientId) throws IOException;

    @Command
    String[] getLatestSnapshot();

    @Command
    String[] getLatestSnapshotByGroup(String groupName);

    @Command
    String[] getSnapshot(long value);

    @Command
    String[] getSnapshotByGroup(String[] data_in);

    @Command(inTypeDesc = "String array where first element is a group name and last elements are attribute full names.")
    void createAttributesGroup(String[] args);
}
