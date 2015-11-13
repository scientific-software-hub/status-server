package wpn.hdri.ss.data2;

import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;
import wpn.hdri.ss.data.Method;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 12.11.2015
 */
public class AllRecordsComplexTest {
    private AllRecords instance;
    private Attribute attr0;
    private Attribute attr1;
    private Attribute attr2;

    /**
     * Setup instance: attr0x6;attr1x3;attr2x1
     */
    @Before
    public void before(){
        instance = new AllRecords(3);

        attr0 = new Attribute(0, null, 0L, Method.EventType.NONE, long.class, "teat-0", "test-0", "test-0");
        attr1 = new Attribute(1, null, 0L, Method.EventType.NONE, double.class, "teat-1", "test-1", "test-1");
        attr2 = new Attribute(2, null, 0L, Method.EventType.NONE, long.class, "teat-2", "test-2", "test-2");


        instance.add(new SingleRecord(attr0, 100L, 0L, 1234L));
        instance.add(new SingleRecord(attr0, 200L, 0L, 1235L));
        instance.add(new SingleRecord(attr0, 300L, 0L, 1236L));
        instance.add(new SingleRecord(attr1, 320L, 0L, 3.14D));
        instance.add(new SingleRecord(attr0, 350L, 0L, 1237L));
        instance.add(new SingleRecord(attr2, 370L, 0L, 789L));
        instance.add(new SingleRecord(attr0, 450L, 0L, 1238L));
        instance.add(new SingleRecord(attr1, 570L, 0L, 3.144D));
        instance.add(new SingleRecord(attr1, 600L, 0L, 3.148D));
    }

    @Test
    public void testGetSnapshot(){
        Iterable<SingleRecord<?>> result = instance.getSnapshot(380L);

        assertArrayEquals(new SingleRecord[]{
                new SingleRecord(attr0, 350L, 0L, 1237L),
                new SingleRecord(attr0, 450L, 0L, 1238L),
                new SingleRecord(attr1, 320L, 0L, 3.14D),
                new SingleRecord(attr1, 570L, 0L, 3.144D),
                new SingleRecord(attr2, 370L, 0L, 789L),
                new SingleRecord(attr2, 370L, 0L, 789L)
        }, Iterables.toArray(result, SingleRecord.class));
    }
}
