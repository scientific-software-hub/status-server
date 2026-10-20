package wpn.hdri.ss.data2;

import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

public class SnapshotTest {
    @Test
    public void emptySlotsAreNullAndIterated() {
        Snapshot snapshot = new Snapshot(3);
        int n = 0;
        for (SingleRecord<?> r : snapshot) {
            assertNull(r);
            n++;
        }
        assertEquals(3, n);
    }

    @Test
    public void iteratorLength() {
        Iterator<SingleRecord<?>> it = new Snapshot(2).iterator();
        assertTrue(it.hasNext());
        it.next();
        it.next();
        assertFalse(it.hasNext());
    }
}
