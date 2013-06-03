package wpn.hdri.ss.engine;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 29.11.12
 */
public class ActivityContext {
    private final List<ReadAttributeTask> pollTasks = new ArrayList<ReadAttributeTask>();
    private final List<ReadAttributeTask> eventTasks = new ArrayList<ReadAttributeTask>();
    private final List<ReadAttributeTask> subscribedTasks = new ArrayList<ReadAttributeTask>();
    private final Collection<ScheduledFuture<?>> runningTasks = new ArrayList<ScheduledFuture<?>>();

    public void addPollTask(ReadAttributeTask task){
        pollTasks.add(task);
    }

    public void addEventTask(ReadAttributeTask task){
        eventTasks.add(task);
    }

    public void addSubscribedTask(ReadAttributeTask task){
        subscribedTasks.add(task);
    }

    public void addRunningTask(ScheduledFuture<?> task){
        runningTasks.add(task);
    }

    public List<ReadAttributeTask> getAllTasks(){
        List<ReadAttributeTask> result = new ArrayList<ReadAttributeTask>(pollTasks);
        result.addAll(eventTasks);
        return result;
    }

    public List<ReadAttributeTask> getPollTasks() {
        return pollTasks;
    }

    public List<ReadAttributeTask> getEventTasks() {
        return eventTasks;
    }

    public List<ReadAttributeTask> getSubscribedTasks(){
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
