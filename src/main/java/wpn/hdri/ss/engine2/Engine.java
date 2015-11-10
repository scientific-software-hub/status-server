package wpn.hdri.ss.engine2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.data2.Attribute;

import java.util.List;
import java.util.concurrent.*;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 10.11.2015
 */
public class Engine {
    private final static Logger logger = LoggerFactory.getLogger(Engine.class);

    public final ScheduledExecutorService exec;

    private volatile String status;

    private final DataStorage storage;

    private final List<Attribute> polledAttributes;
    private final List<Attribute> eventDrivenAttributes;

    private final ConcurrentSkipListSet<ScheduledFuture<?>> runningTasks = new ConcurrentSkipListSet<>();

    public Engine(ScheduledExecutorService exec, DataStorage storage,
                  List<Attribute> polledAttributes, List<Attribute> eventDrivenAttributes){
        this.exec = exec;
        this.storage = storage;
        this.polledAttributes = polledAttributes;
        this.eventDrivenAttributes = eventDrivenAttributes;
    }


    public void start() {
        status = "HEAVY_DUTY";
        for(Attribute attr : polledAttributes){
            runningTasks.add(exec.schedule(new PollTask(storage, attr, append), attr.delay, TimeUnit.MILLISECONDS));
        }
    }

    public void stop(){
        for(ScheduledFuture<?> task : runningTasks){
            task.cancel(false);
        }
    }
}
