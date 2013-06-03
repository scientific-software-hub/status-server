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
import org.apache.log4j.Logger;
import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.client.ClientException;
import wpn.hdri.ss.configuration.Device;
import wpn.hdri.ss.configuration.DeviceAttribute;
import wpn.hdri.ss.configuration.StatusServerAttribute;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.data.*;
import wpn.hdri.ss.storage.Storage;
import wpn.hdri.ss.storage.StorageFactory;
import wpn.hdri.tango.data.type.TangoDataType;
import wpn.hdri.tango.data.type.TangoDataTypes;

import javax.annotation.concurrent.NotThreadSafe;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;
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
    public static final Logger DEFAULT_LOGGER = Logger.getLogger(Engine.class);
    /**
     * This one is used as a multiplicator for Math.random in {@link this#scheduleTasks(java.util.Collection, int)}
     */
    public static final int MAX_INITIAL_DELAY = 1000;
    public static final long MAX_DELAY = 1000L;

    private final //TODO use guava concurrency
            ScheduledExecutorService scheduler;

    private final StatusServerConfiguration configuration;
    private final StorageFactory storage;
    private final ClientsManager clientsManager;
    private final AttributesManager attributesManager;

    private volatile Activity crtActivity = Activity.IDLE;
    private final ActivityContext activityCtx = new ActivityContext();

    private final Logger logger;

    private final EngineInitializer initializer;

    /**
     * Used to determine initial delay for the submitted tasks
     */
    private final Random rnd = new Random();


    /**
     * @param configuration
     * @param storage
     * @param clientsManager
     * @param attributesManager
     * @param logger
     * @param cpus
     */
    public Engine(StatusServerConfiguration configuration, StorageFactory storage, ClientsManager clientsManager, AttributesManager attributesManager, Logger logger, int cpus) {
        this.configuration = configuration;
        this.storage = storage;
        this.clientsManager = clientsManager;
        this.attributesManager = attributesManager;
        this.logger = logger;

        this.initializer = new EngineInitializer();
        this.scheduler = Executors.newScheduledThreadPool(cpus);
    }

    /**
     * Creates new {@link Engine} instance using {@link this#DEFAULT_LOGGER}
     *
     * @param configuration
     * @param storage
     * @param clientsManager
     * @param attributesManager
     * @param cpus
     */
    public Engine(StatusServerConfiguration configuration, Storage storage, ClientsManager clientsManager, AttributesManager attributesManager, int cpus) {
        this(configuration, storage, clientsManager, attributesManager, DEFAULT_LOGGER, cpus);
    }

    public void initialize() {
        logger.info(new SimpleDateFormat("dd MMM yy HH:mm").format(new Date()) + " Engine initialization process started.");
        initializer.initialize();

        logger.info("Finish engine initialization process.");
    }

    /**
     * Returns all attributes values which were written after a timestamp
     *
     * @param timestamp older value will be discarded. May be null - all stored values will be returned
     * @param filter
     * @return a multimap: attribute full name -> attribute values
     */
    public Multimap<AttributeName, AttributeValue<?>> getAllAttributeValues(Timestamp timestamp, AttributeFilter filter) {
        if (timestamp == null) {
            timestamp = Timestamp.DEEP_PAST;
        }
        return attributesManager.takeAllAttributeValues(timestamp, filter);
    }

    /**
     * Returns values bounded by timestamp
     *
     * @param timestamp
     * @param filter
     * @return attribute full name -> attribute value @ timestamp
     * @throws NullPointerException if any of arguments is null
     */
    public Multimap<AttributeName, AttributeValue<?>> getValues(Timestamp timestamp, AttributeFilter filter) {
        Preconditions.checkNotNull(timestamp);
        Preconditions.checkNotNull(filter);
        return attributesManager.takeSnapshot(timestamp, filter);
    }

    public String getInfo() {
        //TODO
        return null;
    }

    /**
     * Cancels all currently running tasks and shutdowns all background threads.
     */
    public void shutdown() {
        logger.info("Shutting down engine...");
        List<Runnable> awaitingTasks = scheduler.shutdownNow();
        logger.info(awaitingTasks.size() + " awaiting tasks cancelled.");
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
        logger.info("Starting...");

        crtActivity = Activity.HEAVY_DUTY;
        crtActivity.start(scheduler, activityCtx, null, logger);
    }

    /**
     * Sets state to not-running, cancels all polling tasks.
     *
     * @throws IllegalStateException if engine is not running
     */
    public synchronized void stop() {
        logger.info("Stopping...");

        crtActivity = Activity.IDLE;
        crtActivity.start(scheduler, activityCtx, null, logger);
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
        crtActivity.start(scheduler, activityCtx, null, logger);
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
        crtActivity.start(scheduler, activityCtx, settings, logger);
    }

    /**
     * Clears all the previously collected data
     */
    public synchronized void clear() {
        Preconditions.checkState(isNotRunning(), "Can not eraseData while current activity is not IDLE");
        logger.info("Erase all data.");
        attributesManager.clear();
    }

    private boolean isNotRunning() {
        return crtActivity == Activity.IDLE;
    }

    public void createAttributesGroup(String groupName, Collection<String> attrFullNames) {
        attributesManager.createAttributesGroup(groupName, attrFullNames);
    }

    public Multimap<AttributeName, AttributeValue<?>> getLatestValues(AttributeFilter filter) {
        Preconditions.checkNotNull(filter);
        return attributesManager.takeLatestSnapshot(filter);
    }

    public <T> void writeAttributeValue(String attrName, T data, Timestamp timestamp) {
        Attribute<T> attr = (Attribute<T>) attributesManager.getAttribute(attrName);
        attr.addValue(timestamp, Value.<T>getInstance(data), Timestamp.now());
    }

    /**
     * Encapsulates initialization logic of this engine.
     */
    private class EngineInitializer {
        private void initialize() {
            initializeStatusServerAttributes();

            initializeClients();

            initializeAttributes();

            initializeReadAttributeTasks();
        }

        private void initializeStatusServerAttributes() {
            for (StatusServerAttribute attr : configuration.getStatusServerAttributes()) {
                logger.info("Initializing embedded attribute " + attr.getName());
                TangoDataType<?> dataType = TangoDataTypes.forString(attr.getType());
                attributesManager.initializeAttribute(attr.asDeviceAttribute(), "", null, dataType.getDataType(), false);
                logger.info("Initialization succeed.");
            }
        }

        private void initializeClients() {
            for (Device dev : configuration.getDevices()) {
                String devName = dev.getName();
                try {
                    logger.info("Initializing client " + devName);
                    clientsManager.initializeClient(devName);
                } catch (ClientInitializationException e) {
                    logger.error("Client initialization failed.", e);
                    clientsManager.reportBadClient(devName, e.getMessage());
                }
            }
        }

        private void initializeAttributes() {
            for (Device dev : configuration.getDevices()) {
                String devName = dev.getName();

                final Client devClient = clientsManager.getClient(devName);

                for (DeviceAttribute attr : dev.getAttributes()) {
                    final String fullName = devName + "/" + attr.getName();
                    logger.info("Initializing attribute " + fullName);
                    boolean isAttrOk = devClient.checkAttribute(attr.getName());
                    if (!isAttrOk) {
                        logger.error("DevClient reports bad attribute: " + fullName);
                        attributesManager.reportBadAttribute(fullName, "Attribute initialization failed.");
                        continue;
                    }
                    devClient.printAttributeInfo(attr.getName(), logger);
                    try {
                        Class<?> attributeClass = devClient.getAttributeClass(attr.getName());
                        boolean isArray = devClient.isArrayAttribute(attr.getName());
                        attributesManager.initializeAttribute(attr, dev.getName(), devClient, attributeClass, isArray);
                        logger.info("Initialization succeed.");
                    } catch (ClientException e) {
                        logger.error("Attribute initialization failed.", e);
                        attributesManager.reportBadAttribute(fullName, e.getMessage());
                    }
                }
            }
        }

        private void initializeReadAttributeTasks() {
            initializePollTasks();

            initializeEventTasks();
        }

        private void initializeEventTasks() {
            for (Attribute<?> attribute : attributesManager.getAttributesByMethod(Method.EVENT)) {
                final Client devClient = clientsManager.getClient(attribute.getName().getDeviceName());
                activityCtx.addEventTask(new ReadAttributeTask(attribute, devClient, 0L, logger));
            }
        }

        private void initializePollTasks() {
            for (final Attribute<?> attribute : attributesManager.getAttributesByMethod(Method.POLL)) {
                DeviceAttribute attr = configuration.getDeviceAttribute(attribute.getName().getDeviceName(), attribute.getName().getName());
                final Client devClient = clientsManager.getClient(attribute.getName().getDeviceName());
                activityCtx.addPollTask(new ReadAttributeTask(attribute, devClient, attr.getDelay(), logger));
            }
        }
    }
}
