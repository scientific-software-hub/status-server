package wpn.hdri.ss.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 29.11.12
 */
public class ActivityContext {
    private final List<PollingReadAttributeTask> pollTasks = new ArrayList<PollingReadAttributeTask>();
    private final List<EventReadAttributeTask> eventTasks = new ArrayList<EventReadAttributeTask>();
    private final List<EventReadAttributeTask> subscribedTasks = new ArrayList<EventReadAttributeTask>();
    private final Collection<ScheduledFuture<?>> runningTasks = new ArrayList<ScheduledFuture<?>>();

    public void addPollTask(PollingReadAttributeTask task){
        pollTasks.add(task);
    }

    public void addEventTask(EventReadAttributeTask task){
        eventTasks.add(task);
    }

    public void addSubscribedTask(EventReadAttributeTask task){
        subscribedTasks.add(task);
    }

    public void addRunningTask(ScheduledFuture<?> task){
        runningTasks.add(task);
    }

    public List<PollingReadAttributeTask> getPollTasks() {
        return pollTasks;
    }

    public List<EventReadAttributeTask> getEventTasks() {
        return eventTasks;
    }

    public List<EventReadAttributeTask> getSubscribedTasks(){
        return subscribedTasks;
    }

    public List<ScheduledFuture<?>> getRunningTasks(){
        return (List<ScheduledFuture<?>>) runningTasks;
    }

    public void clearAllTasks(){
        pollTasks.clear();
        eventTasks.clear();
        runningTasks.clear();
    }
}
