package wpn.hdri.ss.data2;

import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;
import wpn.hdri.ss.data.Method;

import java.util.Arrays;

import static org.junit.Assert.*;

public class AllRecordsTest {

    private AllRecords instance;

    @Before
    public void before(){
        instance = new AllRecords(1);

        for(int i = 0; i < 10; ++i){
            instance.add(new SingleRecord(Attributes.ATTR0, i*100,0L,1234L));
        }
    }

    @Test
    public void testGet_51() throws Exception {
        Iterable<SingleRecord<?>> result = instance.getRange(510);

        assertTrue(Iterables.elementsEqual(
                Arrays.<SingleRecord>asList(
                new SingleRecord(null,600,0L,1234L),
                new SingleRecord(null,700,0L,1234L),
                new SingleRecord(null,800,0L,1234L),
                new SingleRecord(null,900,0L,1234L)
        ), result));
    }

    @Test
    public void testGet_19() throws Exception {
        Iterable<SingleRecord<?>> result = instance.getRange(190);
        assertTrue(Iterables.elementsEqual(Arrays.asList(
                new SingleRecord(null, 200, 0L, 1234L),
                new SingleRecord(null, 300, 0L, 1234L),
                new SingleRecord(null, 400, 0L, 1234L),
                new SingleRecord(null, 500, 0L, 1234L),
                new SingleRecord(null, 600, 0L, 1234L),
                new SingleRecord(null, 700, 0L, 1234L),
                new SingleRecord(null, 800, 0L, 1234L),
                new SingleRecord(null, 900, 0L, 1234L)
        ), result));
    }

    @Test
    public void testGet_100() throws Exception {
        Iterable<SingleRecord<?>> result = instance.getRange(1000);
        assertTrue(Iterables.size(result) == 0);
    }

    @Test
    public void testGet_0() throws Exception {
        Iterable<SingleRecord<?>> result = instance.getRange(0);
        assertTrue(Iterables.elementsEqual(Arrays.asList(
                new SingleRecord(null, 0, 0L, 1234L),
                new SingleRecord(null, 100, 0L, 1234L),
                new SingleRecord(null, 200, 0L, 1234L),
                new SingleRecord(null, 300, 0L, 1234L),
                new SingleRecord(null, 400, 0L, 1234L),
                new SingleRecord(null, 500, 0L, 1234L),
                new SingleRecord(null, 600, 0L, 1234L),
                new SingleRecord(null, 700, 0L, 1234L),
                new SingleRecord(null, 800, 0L, 1234L),
                new SingleRecord(null, 900, 0L, 1234L)
        ), result));
    }

    @Test
    public void testGetRange_All() throws Exception {
        Iterable<SingleRecord<?>> result = instance.getRange(0,1000);

        assertTrue(Iterables.elementsEqual(Arrays.asList(
                new SingleRecord(null,  0, 0L, 1234L),
                new SingleRecord(null, 100, 0L, 1234L),
                new SingleRecord(null, 200, 0L, 1234L),
                new SingleRecord(null, 300, 0L, 1234L),
                new SingleRecord(null, 400, 0L, 1234L),
                new SingleRecord(null, 500, 0L, 1234L),
                new SingleRecord(null, 600, 0L, 1234L),
                new SingleRecord(null, 700, 0L, 1234L),
                new SingleRecord(null, 800, 0L, 1234L),
                new SingleRecord(null, 900, 0L, 1234L)
        ), result));
    }

    @Test
    public void testGetRange_19_21() throws Exception {
        Iterable<SingleRecord<?>> result = instance.getRange(190,210);

        assertTrue(Iterables.elementsEqual(Arrays.asList(
                new SingleRecord(null, 200, 0L, 1234L)
                ), result));
    }

    @Test
    public void testGetRange_0_21() throws Exception {
        Iterable<SingleRecord<?>> result = instance.getRange(0,210);


        assertTrue(Iterables.elementsEqual(Arrays.asList(
                new SingleRecord(null, 0, 0L, 1234L),
                new SingleRecord(null, 100, 0L, 1234L),
                new SingleRecord(null, 200, 0L, 1234L)
        ), result));
    }

    @Test
    public void testGetRange_41_100() throws Exception {
        Iterable<SingleRecord<?>> result = instance.getRange(410,1000);

        assertTrue(Iterables.elementsEqual(Arrays.asList(
        new SingleRecord(null, 500, 0L, 1234L),
                new SingleRecord(null, 600, 0L, 1234L),
                new SingleRecord(null, 700, 0L, 1234L),
                new SingleRecord(null, 800, 0L, 1234L),
                new SingleRecord(null, 900, 0L, 1234L)

        ), result));
    }

    @Test
    public void testGetRange_91_100() throws Exception {
        Iterable<SingleRecord<?>> result = instance.getRange(910,1000);

        assertTrue(Iterables.size(result) == 0);
    }

    @Test
    public void testGetRange_51_101() throws Exception {
        instance.add(new SingleRecord(Attributes.ATTR0, 1000L, 0L, 1234L));

        Iterable<SingleRecord<?>> result = instance.getRange(510,1010);

        assertTrue(Iterables.elementsEqual(Arrays.asList(
                new SingleRecord(null, 600, 0L, 1234L),
                new SingleRecord(null, 700, 0L, 1234L),
                new SingleRecord(null, 800, 0L, 1234L),
                new SingleRecord(null, 900, 0L, 1234L),
                new SingleRecord(null, 1000, 0L, 1234L)
        ), result));
    }

    @Test
    public void testGetRange_41_71() throws Exception {
        Iterable<SingleRecord<?>> result = instance.getRange(410,710);

        assertTrue(Iterables.elementsEqual(Arrays.asList(
        new SingleRecord(null, 500, 0L, 1234L),
                new SingleRecord(null, 600, 0L, 1234L),
                new SingleRecord(null, 700, 0L, 1234L)
                ), result));
    }

    @Test
    public void testGetRange_71_100() throws Exception {
        Iterable<SingleRecord<?>> result = instance.getRange(710,1000);

        assertTrue(Iterables.elementsEqual(Arrays.asList(
                new SingleRecord(null, 800, 0L, 1234L),
                new SingleRecord(null, 900, 0L, 1234L)
        ), result));
    }

    @Test
    public void testNotFound(){
        instance = new AllRecords(1);

        instance.add(new SingleRecord(Attributes.ATTR0, 1000L, 0L, 1234L));
        instance.add(new SingleRecord(Attributes.ATTR0, 2000L, 0L, 1234L));
        instance.add(new SingleRecord(Attributes.ATTR0, 3000L, 0L, 1234L));

        Iterable<SingleRecord<?>> result = instance.getRange(2500L);

        assertTrue(Iterables.elementsEqual(Arrays.asList(new SingleRecord(null, 3000L, 0L, 1234L)), result));
    }

}