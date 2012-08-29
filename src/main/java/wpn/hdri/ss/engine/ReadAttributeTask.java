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

import org.apache.log4j.Logger;
import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.data.Attribute;
import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.data.Value;
import wpn.hdri.tango.proxy.EventData;
import wpn.hdri.tango.proxy.TangoEventCallback;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performs reading task. Reads attribute from the device server.
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 02.05.12
 */
public final class ReadAttributeTask implements Runnable, TangoEventCallback<Object> {
    private final Attribute<?> attribute;
    private final Client devClient;

    private final Logger logger;

    private final long delay;

    /**
     * After each failed read attempt this will be multiplied by delay to get a delay before next attempt
     */
    private final AtomicLong tries = new AtomicLong(0L);
    /**
     * Defines a number of tries this read task will attempt before throw an exception
     */
    public final static long MAX_TRIES = 10L;

    public ReadAttributeTask(Attribute<?> attribute, Client devClient, long delay, Logger logger) {
        this.attribute = attribute;
        this.devClient = devClient;
        this.delay = delay;
        this.logger = logger;
    }

    /**
     * Fired from successful event read attempt.
     *
     * @param eventData new value
     */
    public final void onEvent(EventData<Object> eventData) {
        Timestamp timestamp = getCurrentMilliseconds();
        attribute.addValue(timestamp, Value.getInstance(eventData.getValue()), new Timestamp(eventData.getTime()));
    }

    /**
     * Fired from failed event read attempt.
     *
     * @param ex cause
     */
    public final void onError(Throwable ex) {
        logger.error("Can not read from " + attribute.getFullName(), ex);
        Timestamp timestamp = getCurrentMilliseconds();

        attribute.addValue(timestamp, Value.NULL, timestamp);
    }

    /**
     * Performs poll task.
     */
    @Override
    public void run() {
        try {
            Map.Entry<Object, Timestamp> result = devClient.readAttribute(attribute.getName());
            Object data = result.getKey();
            //uncomment this will produce a huge number of Strings. So it is not recommended in production
            //logger.info("Read attribute " + attribute.getFullName() + ": " + data);

            attribute.addValue(getCurrentMilliseconds(), Value.getInstance(data), result.getValue());
            tries.set(0L);
        } catch (Throwable e) {
            if (tries.incrementAndGet() < MAX_TRIES) {
                logger.error("An attempt to read attribute " + attribute.getFullName() + " has failed. Tries left: " + (MAX_TRIES - tries.get()), e);
                long delay = getDelay() + tries.get() * getDelay();
                logger.error("Next try in " + delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                }
            } else {
                logger.error("All attempts to read attribute " + attribute.getFullName() + " failed. Writing null.", e);

                attribute.addValue(getCurrentMilliseconds(), Value.NULL, getCurrentMilliseconds());
            }
        }
    }

    private Timestamp getNanoTime() {
        long nanoTime = System.nanoTime();

        return new Timestamp(nanoTime, TimeUnit.NANOSECONDS);
    }

    private Timestamp getCurrentMilliseconds() {
        long millis = System.currentTimeMillis();

        return new Timestamp(millis);
    }

    public long getDelay() {
        return delay;
    }

    public Attribute<?> getAttribute() {
        return attribute;
    }
}
