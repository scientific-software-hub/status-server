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
import org.junit.Test;
import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.data.Attribute;
import wpn.hdri.ss.data.Timestamp;

import java.util.AbstractMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 04.05.12
 */
public class ReadAttributeTaskTest {
    @Test
    public void testCancellation() throws Exception {
        Client mockClient = mock(Client.class);
        stub(mockClient.readAttribute(null)).toReturn(new AbstractMap.SimpleEntry<java.lang.Object, wpn.hdri.ss.data.Timestamp>(null, new Timestamp(System.currentTimeMillis())));
        Attribute mockAttribute = mock(Attribute.class);
        Logger mockLogger = mock(Logger.class);

        ScheduledExecutorService singleThreadExecutor = Executors.newScheduledThreadPool(1);

        ReadAttributeTask command = new ReadAttributeTask(mockAttribute, mockClient, 0, mockLogger);
        ScheduledFuture<?> task = singleThreadExecutor.scheduleAtFixedRate(command, 0, Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        Thread.sleep(50);

        List<Runnable> awaitingTasks = singleThreadExecutor.shutdownNow();
        assertEquals(1, awaitingTasks.size());
        //TODO rewrite this test as we can not rely on logger any more (it was disabled due to performance issues)
        //verify(mockLogger, atLeastOnce()).info("Read attribute null: null");
    }
}
