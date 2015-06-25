/*
 * The main contributor to this project is Institute of Materials Research,
 * Helmholtz-Zentrum Geesthacht,
 * Germany.
 *
 * This project is a contribution of the Helmholtz Association Centres and
 * Technische Universitaet Muenchen to the ESS Design Update Phase.
 *
 * The project's funding reference is FKZ05E11CG1.
 *
 * Copyright (c) 2012. Institute of Materials Research,
 * Helmholtz-Zentrum Geesthacht,
 * Germany.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package wpn.hdri.ss.engine;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.data.Value;
import wpn.hdri.ss.data.attribute.Attribute;
import wpn.hdri.ss.data.attribute.AttributeName;
import wpn.hdri.ss.data.attribute.AttributeValue;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Engine should never fail during its work. It may fail during initialization.
 * <p/>
 * Not thread safe. Designed to be thread confinement.
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 27.04.12
 */
//TODO replace Preconditions with StateMachine and meaningful exceptions
@NotThreadSafe
public class Engine {
    /**
     * By default engine's logger stores log in {APP_ROOT}/logs/engine.out
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(Engine.class);
    /**
     * This one is used as a multiplicator for Math.random in {@link this#scheduler}
     */
    public static final int MAX_INITIAL_DELAY = 1000;

    private final //TODO use guava concurrency
            ScheduledExecutorService scheduler;

    private final ClientsManager clientsManager;
    private final AttributesManager attributesManager;
    private final ActivityContext activityCtx = new ActivityContext();
    private volatile Activity crtActivity = Activity.IDLE;

    /**
     * @param clientsManager
     * @param attributesManager
     * @param threads           how many thread will be utilized by the engine
     */
    public Engine(ClientsManager clientsManager, AttributesManager attributesManager, int threads) {
        this.clientsManager = clientsManager;
        this.attributesManager = attributesManager;

        this.scheduler = Executors.newScheduledThreadPool(threads);
    }

    public Engine(EngineInitializationContext ctx) {
        this(ctx.clientsManager, ctx.attributesManager, ctx.properties.engineCpus);
        submitPollingTasks(ctx.pollingTasks);
        submitEventTasks(ctx.eventTasks);
    }

    /**
     * Returns all attributes values which were written after a timestamp
     *
     * @param timestamp older value will be discarded. May be null - all stored values will be returned
     * @param group
     * @return a multimap: attribute full name -> attribute values
     */
    public Multimap<AttributeName, AttributeValue<?>> getAllAttributeValues(@Nullable Timestamp timestamp, String group) {
        if (timestamp == null) {
            timestamp = Timestamp.DEEP_PAST;
        }
        return attributesManager.takeAllAttributeValues(timestamp, group);
    }

    /**
     * Returns values bounded by timestamp
     *
     * @param timestamp
     * @param group
     * @return attribute full name -> attribute value @ timestamp
     * @throws NullPointerException if any of arguments is null
     */
    public Multimap<AttributeName, AttributeValue<?>> getValues(Timestamp timestamp, String group) {
        return attributesManager.takeSnapshot(timestamp, group);
    }

    /**
     * Cancels all currently running tasks and shutdowns all background threads.
     */
    public void shutdown() {
        LOGGER.info("Shutting down engine...");
        List<Runnable> awaitingTasks = scheduler.shutdownNow();
        LOGGER.info(awaitingTasks.size() + " awaiting tasks cancelled.");
    }

    /**
     * Sets state to running, schedules all polling tasks, subscribes to events.
     *
     * @throws IllegalStateException if engine is already running
     */
    public /*synchronized*/ void start() {
        start(MAX_INITIAL_DELAY);
    }

    /**
     * For internal use only.
     *
     * @param taskInitialDelay actual delay will be randomly chosen starting from '0' to this value exclusively
     */
    synchronized void start(int taskInitialDelay) {
        Preconditions.checkState(isNotRunning(), "Can not start collectData while current activity is not IDLE");
        LOGGER.info("Starting...");

        crtActivity = Activity.HEAVY_DUTY;
        crtActivity.start(scheduler, activityCtx, null, LOGGER);
    }

    /**
     * Sets state to not-running, cancels all polling tasks.
     *
     * @throws IllegalStateException if engine is not running
     */
    public synchronized void stop() {
        LOGGER.info("Stopping...");

        crtActivity = Activity.IDLE;
        crtActivity.start(scheduler, activityCtx, null, LOGGER);
    }

    public String getCurrentActivity() {
        return crtActivity.name();
    }

    /**
     * Submits poll tasks for all attributes. These tasks will clear attribute before adding new data.
     * <p/>
     * All polls will be performed at rate of 1 s.
     */
    public void startLightPolling() {
        Preconditions.checkState(isNotRunning(), "Can not start lightPolling while current activity is not IDLE");

        crtActivity = Activity.LIGHT_POLLING;
        crtActivity.start(scheduler, activityCtx, null, LOGGER);
    }

    /**
     * Submits poll tasks for all attributes. These tasks will clear attribute before adding new data.
     * <p/>
     * All polls will be performed at rate of 1 s.
     */
    public void startLightPollingAtFixedRate(long delay) {
        Preconditions.checkState(isNotRunning(), "Can not start lightPolling while current activity is not IDLE");

        ActivitySettings settings = new ActivitySettings(delay, MAX_INITIAL_DELAY);

        crtActivity = Activity.LIGHT_POLLING;
        crtActivity.start(scheduler, activityCtx, settings, LOGGER);
    }

    /**
     * Clears all the previously collected data
     */
    public synchronized void clear() {
        Preconditions.checkState(isNotRunning(), "Can not eraseData while current activity is not IDLE");
        LOGGER.info("Erasing all data.");
        attributesManager.clear();
    }

    private boolean isNotRunning() {
        return crtActivity == Activity.IDLE;
    }

    public void createAttributesGroup(String groupName, Collection<String> attrFullNames) {
        //TODO replace full names with AttributeName objects
        attributesManager.createAttributesGroup(groupName, attrFullNames);
    }

    public Multimap<AttributeName, AttributeValue<?>> getLatestValues(String group) {
        return attributesManager.takeLatestSnapshot(group);
    }

    public <T> void writeAttributeValue(String attrName, T data, Timestamp timestamp) {
        Attribute<T> attr = (Attribute<T>) attributesManager.getAttribute(attrName);
        attr.addValue(timestamp, Value.<T>getInstance(data), Timestamp.now());
    }

    public void submitPollingTasks(List<PollingReadAttributeTask> pollingTasks) {
        for (PollingReadAttributeTask task : pollingTasks) {
            activityCtx.addPollTask(task);
        }
    }

    public void submitEventTasks(List<EventReadAttributeTask> eventTasks) {
        for (EventReadAttributeTask task : eventTasks) {
            activityCtx.addEventTask(task);
        }
    }

    public Iterable<Map.Entry<AttributeName, Class<?>>> getAttributeClasses() {
        return attributesManager.getAttributeClasses();
    }

    public Multimap<AttributeName, AttributeValue<?>> getValuesRange(Timestamp from, Timestamp to, String group) {
        return attributesManager.takeAttributeValues(from, to, group);
    }
}
