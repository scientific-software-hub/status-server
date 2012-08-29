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
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.data.Attribute;
import wpn.hdri.ss.data.AttributeValue;
import wpn.hdri.ss.data.Method;
import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.storage.Storage;

import javax.annotation.concurrent.NotThreadSafe;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

    private final //TODO use guava concurrency
            ScheduledExecutorService pollingService;

    private final StatusServerConfiguration configuration;
    private final Storage storage;
    private final ClientsManager clientsManager;
    private final AttributesManager attributesManager;

    private final Logger logger;

    private final List<ReadAttributeTask> pollingTasks = new ArrayList<ReadAttributeTask>();
    private final List<ReadAttributeTask> eventTasks = new ArrayList<ReadAttributeTask>();

    private volatile boolean running = false;

    private final Collection<ScheduledFuture<?>> runningTasks = new ArrayList<ScheduledFuture<?>>();
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
    public Engine(StatusServerConfiguration configuration, Storage storage, ClientsManager clientsManager, AttributesManager attributesManager, Logger logger, int cpus) {
        this.configuration = configuration;
        this.storage = storage;
        this.clientsManager = clientsManager;
        this.attributesManager = attributesManager;
        this.logger = logger;

        this.initializer = new EngineInitializer();
        this.pollingService = Executors.newScheduledThreadPool(cpus);
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
    public Multimap<String, AttributeValue<?>> getAllAttributeValues(Timestamp timestamp, AttributeFilter filter) {
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
    public Collection<AttributeValue<?>> getValues(Timestamp timestamp, AttributeFilter filter) {
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
        List<Runnable> awaitingTasks = pollingService.shutdownNow();
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
        Preconditions.checkState(!running);
        logger.info("Starting...");

        //cancel light polling tasks
        cancelScheduledTasks(runningTasks);

        scheduleTasks(pollingTasks, taskInitialDelay);
        subscribeEventTasks(eventTasks);

        running = true;
    }

    /**
     * Sets state to not-running, cancels all polling tasks.
     *
     * @throws IllegalStateException if engine is not running
     */
    public synchronized void stop() {
        Preconditions.checkState(running);
        logger.info("Stopping...");

        cancelScheduledTasks(runningTasks);
        unsubscribeEventTasks(eventTasks);

        running = false;
    }

    /**
     * Submits poll tasks for all attributes. These tasks will clear attribute before adding new data.
     * <p/>
     * All polls will be performed at rate of 1 s.
     */
    public void startLightPolling() {
        Preconditions.checkState(!running);
        logger.info("Start light polling.");

        cancelScheduledTasks(runningTasks);
        for (final Attribute<?> attribute : attributesManager.getAttributes()) {
            final Client client = clientsManager.getClient(attribute.getDeviceName());

            logger.info("Scheduling light polling task for " + attribute.getFullName());
            runningTasks.add(
                    pollingService.scheduleAtFixedRate(
                            new Runnable() {
                                private final ReadAttributeTask innerTask = new ReadAttributeTask(attribute, client, 1000L, logger);

                                @Override
                                public void run() {
                                    if (innerTask.getAttribute().getAttributeValue() != null) {
                                        innerTask.getAttribute().clear();
                                    }
                                    innerTask.run();
                                }
                            }, rnd.nextInt(MAX_INITIAL_DELAY), 1000L, TimeUnit.MILLISECONDS));

        }
    }

    private void unsubscribeEventTasks(List<ReadAttributeTask> tasks) {
        for (ReadAttributeTask task : tasks) {
            try {
                logger.info("Unsubscribing from " + task.getAttribute().getFullName());
                Client devClient = clientsManager.getClient(task.getAttribute().getDeviceName());
                devClient.unsubscribeEvent(task.getAttribute().getName());
            } catch (ClientException e) {
                logger.error("Event unsubscription failed.", e);
                attributesManager.reportBadAttribute(task.getAttribute().getFullName(), e.getMessage());
            }
        }
    }

    private void cancelScheduledTasks(Collection<ScheduledFuture<?>> tasks) {
        for (ScheduledFuture<?> task : tasks) {
            logger.info("Canceling scheduled task...");
            task.cancel(false);
        }
        tasks.clear();
    }

    private void scheduleTasks(Collection<ReadAttributeTask> tasks, int taskInitialDelay) {
        for (final ReadAttributeTask task : tasks) {
            logger.info("Scheduling read task for " + task.getAttribute().getFullName());
            runningTasks.add(pollingService.scheduleWithFixedDelay(task, rnd.nextInt(taskInitialDelay), task.getDelay(), TimeUnit.MILLISECONDS));
        }
    }

    private void subscribeEventTasks(Collection<ReadAttributeTask> tasks) {
        for (ReadAttributeTask task : tasks) {
            try {
                logger.info("Subscribing for changes from " + task.getAttribute().getFullName());
                Client devClient = clientsManager.getClient(task.getAttribute().getDeviceName());
                devClient.subscribeEvent(task.getAttribute().getName(), task);
            } catch (ClientException e) {
                logger.error("Event subscription failed.", e);
                attributesManager.reportBadAttribute(task.getAttribute().getFullName(), e.getMessage());
            }
        }
    }

    /**
     * Clears all the previously collected data
     */
    public synchronized void clear() {
        Preconditions.checkState(!running);
        logger.info("Erase all data.");
        attributesManager.clear();
    }

    public void createAttributesGroup(String groupName, Collection<String> attrFullNames) {
        attributesManager.createAttributesGroup(groupName, attrFullNames);
    }

    /**
     * Encapsulates initialization logic of this engine.
     */
    private class EngineInitializer {
        private void initialize() {
            initializeClients();

            initializeAttributes();

            initializeReadAttributeTasks();
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
                        attributesManager.initializeAttribute(attr, dev, devClient, attributeClass);
                        logger.info("Initialization succeed.");
                    } catch (ClientException e) {
                        logger.error("Attribute initialization failed.", e);
                        attributesManager.reportBadAttribute(fullName, e.getMessage());
                    }
                }
            }
        }

        private void initializeReadAttributeTasks() {
            for (final Attribute<?> attribute : attributesManager.getAttributesByMethod(Method.POLL)) {
                DeviceAttribute attr = configuration.getDeviceAttribute(attribute.getDeviceName(), attribute.getName());
                final Client devClient = clientsManager.getClient(attribute.getDeviceName());
                pollingTasks.add(new ReadAttributeTask(attribute, devClient, attr.getDelay(), logger));
            }

            for (Attribute<?> attribute : attributesManager.getAttributesByMethod(Method.EVENT)) {
                final Client devClient = clientsManager.getClient(attribute.getDeviceName());
                eventTasks.add(new ReadAttributeTask(attribute, devClient, 0L, logger));
            }
        }
    }
}
