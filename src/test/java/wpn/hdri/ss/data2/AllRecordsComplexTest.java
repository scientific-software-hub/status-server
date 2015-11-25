package wpn.hdri.ss.data2;

import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.Arrays;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 12.11.2015
 */
public class AllRecordsComplexTest {
    private AllRecords instance;

    /**
     * Setup instance: attr0x6;attr1x3;attr2x1
     */
    @Before
    public void before(){
        instance = new AllRecords(3);

        instance.add(new SingleRecord(Attributes.ATTR0, 100L, 0L, 1234L));
        instance.add(new SingleRecord(Attributes.ATTR0, 200L, 0L, 1235L));
        instance.add(new SingleRecord(Attributes.ATTR0, 300L, 0L, 1236L));
        instance.add(new SingleRecord(Attributes.ATTR1, 320L, 0L, 3.14D));
        instance.add(new SingleRecord(Attributes.ATTR0, 350L, 0L, 1237L));
        instance.add(new SingleRecord(Attributes.ATTR2, 370L, 0L, 789L));
        instance.add(new SingleRecord(Attributes.ATTR0, 450L, 0L, 1238L));
        instance.add(new SingleRecord(Attributes.ATTR1, 570L, 0L, 3.144D));
        instance.add(new SingleRecord(Attributes.ATTR1, 600L, 0L, 3.148D));
    }

    @Test
    public void testGetSnapshot(){
        Iterable<?> result = instance.getSnapshot(380L);

        assertTrue(Iterables.elementsEqual(Arrays.asList(
                new SingleRecord(Attributes.ATTR0, 350L, 0L, 1237L),
                new SingleRecord(Attributes.ATTR1, 320L, 0L, 3.14D),
                new SingleRecord(Attributes.ATTR2, 370L, 0L, 789L)
        ), result));
    }
}
