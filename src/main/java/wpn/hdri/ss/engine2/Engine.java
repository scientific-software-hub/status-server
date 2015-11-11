package wpn.hdri.ss.engine2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.data2.Attribute;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 10.11.2015
 */
public class Engine {
    private final static Logger logger = LoggerFactory.getLogger(Engine.class);

    public final ScheduledExecutorService exec;

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
        logger.debug("Starting...");
        start(true, -1);
        logger.debug("Done!");
    }

    private void start(boolean append, long delay){
        for(Attribute attr : polledAttributes){
            logger.debug("Scheduling polling task for {}", attr.fullName);
            runningTasks.add(
                    exec.scheduleAtFixedRate(
                            new PollTask(attr, storage, true), 0L, delay == -1 ? attr.delay : delay, TimeUnit.MILLISECONDS));
        }
        for(Attribute attr : eventDrivenAttributes){
            logger.debug("Subscribing to {}" , attr.fullName);
            attr.devClient.subscribe(new EventTask(attr, storage, true));
        }
    }

    public void startLightPolling(){
        logger.debug("Starting light polling...");
        start(false, -1);
        logger.debug("Done!");
    }

    public void startLightPollingAtFixedRate(long delay){
        if(delay < 0) throw new IllegalArgumentException("delay must be positive!");
        logger.debug("Starting light polling at fixed rate...");
        start(false, delay);
        logger.debug("Done!");
    }

    public void stop(){
        logger.debug("Stopping...");
        for(ScheduledFuture<?> task : runningTasks){
            logger.debug("Canceling polling task for...");
            task.cancel(false);
        }
        for(Attribute attr : eventDrivenAttributes){
            logger.debug("Unsubscribing from {}", attr.fullName);
            attr.devClient.unsubscribe(attr);
        }
        logger.debug("Done!");
    }

    public DataStorage getStorage(){
        return storage;
    }

    //TODO erase data
}
