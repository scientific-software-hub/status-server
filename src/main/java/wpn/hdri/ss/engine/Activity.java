package wpn.hdri.ss.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.client.ClientException;

import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 29.11.12
 */
public enum Activity {
    LIGHT_POLLING {
        /**
         * Schedules polling tasks at fixed rate if settings is not null. Each scheduled task deletes all previous records before putting a new one.
         * @param scheduler
         * @param ctx
         */
        @Override
        public void start(ScheduledExecutorService scheduler, ActivityContext ctx) {
            LOGGER.debug("Starting light polling...");

            Collection<ScheduledFuture<?>> runningTasks = ctx.getRunningTasks();

            for (final PollingReadAttributeTask task : ctx.getPollTasks()) {
                final Client client = task.getDevClient();

                LOGGER.debug("Scheduling light polling task for {}", task.getAttribute().getFullName());
                runningTasks.add(
                        scheduler.scheduleAtFixedRate(
                                new PollingReadAttributeTask(task.getAttribute(), client, 1000L, false),
                                0L,delay,TimeUnit.MILLISECONDS));

            }

            for (final EventReadAttributeTask task : ctx.getEventTasks()) {
                try {
                    LOGGER.info("Subscribing for changes from {}", task.getAttribute().getFullName());
                    task.getDevClient().subscribeEvent(task.getAttribute().getName().getName(), task.getEventType(), task);
                    ctx.addSubscribedTask(task);
                } catch (ClientException e) {
                    LOGGER.error("Event subscription has failed.", e);
                }
            }
            LOGGER.debug("Light polling has been started!");
        }
    },
    HEAVY_DUTY {
        @Override
        public void start(ScheduledExecutorService scheduler, ActivityContext ctx) {
            schedulePollTasks(ctx.getPollTasks(), ctx.getRunningTasks(), scheduler);
            subscribeEventTasks(ctx.getEventTasks(), ctx.getSubscribedTasks());
        }

        private void schedulePollTasks(Collection<PollingReadAttributeTask> tasks, Collection<ScheduledFuture<?>> runningTasks, ScheduledExecutorService scheduler) {
            for (final PollingReadAttributeTask task : tasks) {
                LOGGER.debug("Scheduling read task for {}", task.getAttribute().getFullName());
                runningTasks.add(scheduler.scheduleWithFixedDelay(task, 0L, task.getDelay(), TimeUnit.MILLISECONDS));
            }
        }

        private void subscribeEventTasks(Collection<EventReadAttributeTask> tasks, Collection<EventReadAttributeTask> subscribedTasks) {
            for (EventReadAttributeTask task : tasks) {
                try {
                    LOGGER.info("Subscribing for changes from {}", task.getAttribute().getFullName());

                    task.getDevClient().subscribeEvent(task.getAttribute().getName().getName(), task.getEventType(), task);
                    subscribedTasks.add(task);
                } catch (ClientException e) {
                    LOGGER.error("Event subscription has failed.", e);
                }
            }
        }
    },
    IDLE {
        @Override
        public void start(ScheduledExecutorService scheduler, ActivityContext ctx) {
            cancelScheduledTasks(ctx.getRunningTasks());
            unsubscribeEventTasks(ctx.getSubscribedTasks());
        }

        private void cancelScheduledTasks(Collection<ScheduledFuture<?>> tasks) {
            for (ScheduledFuture<?> task : tasks) {
                LOGGER.info("Canceling scheduled task...");
                task.cancel(false);
            }
            tasks.clear();
        }

        private void unsubscribeEventTasks(Collection<EventReadAttributeTask> tasks) {
            for (EventReadAttributeTask task : tasks) {
                try {
                    LOGGER.debug("Unsubscribing from {}", task.getAttribute().getFullName());
                    task.getDevClient().unsubscribeEvent(task.getAttribute().getName().getName());
                } catch (ClientException e) {
                    LOGGER.error("Event unsubscription has failed.", e);
                }
            }
            tasks.clear();
        }
    };

    private static final Logger LOGGER = LoggerFactory.getLogger(Activity.class);

    protected long delay;

    public abstract void start(ScheduledExecutorService scheduler, ActivityContext ctx);
}
