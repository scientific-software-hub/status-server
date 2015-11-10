package wpn.hdri.ss.data2;

import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class AllRecordsTest {

    private AllRecords instance;

    @Before
    public void before(){
        instance = new AllRecords();

        for(int i = 0; i < 10; ++i){
            instance.add(new SingleRecord(0,i*10,0L,1234L));
        }
    }

    @Test
    public void testGet_51() throws Exception {
        SingleRecord[] result = instance.get(51);
        assertArrayEquals(new SingleRecord[]{
                new SingleRecord(0,60,0L,1234L),
                new SingleRecord(0,70,0L,1234L),
                new SingleRecord(0,80,0L,1234L),
                new SingleRecord(0,90,0L,1234L)
        }, result);
    }

    @Test
    public void testGet_19() throws Exception {
        SingleRecord[] result = instance.get(19);
        assertArrayEquals(new SingleRecord[]{
                new SingleRecord(0,20,0L,1234L),
                new SingleRecord(0,30,0L,1234L),
                new SingleRecord(0,40,0L,1234L),
                new SingleRecord(0,50,0L,1234L),
                new SingleRecord(0,60,0L,1234L),
                new SingleRecord(0,70,0L,1234L),
                new SingleRecord(0,80,0L,1234L),
                new SingleRecord(0,90,0L,1234L)
        }, result);
    }

    @Test
    public void testGet_100() throws Exception {
        SingleRecord[] result = instance.get(100);
        assertTrue(result.length == 0);
    }

    @Test
    public void testGet_0() throws Exception {
        SingleRecord[] result = instance.get(0);
        assertArrayEquals(new SingleRecord[]{
                new SingleRecord(0,  0, 0L, 1234L),
                new SingleRecord(0, 10, 0L, 1234L),
                new SingleRecord(0, 20, 0L, 1234L),
                new SingleRecord(0, 30, 0L, 1234L),
                new SingleRecord(0, 40, 0L, 1234L),
                new SingleRecord(0, 50, 0L, 1234L),
                new SingleRecord(0, 60, 0L, 1234L),
                new SingleRecord(0, 70, 0L, 1234L),
                new SingleRecord(0, 80, 0L, 1234L),
                new SingleRecord(0, 90, 0L, 1234L)
        }, result);
    }

    @Test
    public void testGetRange_All() throws Exception {
        SingleRecord[] result = instance.getRange(0,100);

        assertArrayEquals(new SingleRecord[]{
                new SingleRecord(0,  0, 0L, 1234L),
                new SingleRecord(0, 10, 0L, 1234L),
                new SingleRecord(0, 20, 0L, 1234L),
                new SingleRecord(0, 30, 0L, 1234L),
                new SingleRecord(0, 40, 0L, 1234L),
                new SingleRecord(0, 50, 0L, 1234L),
                new SingleRecord(0, 60, 0L, 1234L),
                new SingleRecord(0, 70, 0L, 1234L),
                new SingleRecord(0, 80, 0L, 1234L),
                new SingleRecord(0, 90, 0L, 1234L)
        }, result);
    }

    @Test
    public void testGetRange_19_21() throws Exception {
        SingleRecord[] result = instance.getRange(19,21);

        assertArrayEquals(new SingleRecord[]{
                new SingleRecord(0, 20, 0L, 1234L),
        }, result);
    }

    @Test
    public void testGetRange_0_21() throws Exception {
        SingleRecord[] result = instance.getRange(0,21);


        assertArrayEquals(new SingleRecord[]{
                new SingleRecord(0,  0, 0L, 1234L),
                new SingleRecord(0, 10, 0L, 1234L),
                new SingleRecord(0, 20, 0L, 1234L)
        }, result);
    }

    @Test
    public void testGetRange_41_100() throws Exception {
        SingleRecord[] result = instance.getRange(41,100);

        assertArrayEquals(new SingleRecord[]{
                new SingleRecord(0, 50, 0L, 1234L),
                new SingleRecord(0, 60, 0L, 1234L),
                new SingleRecord(0, 70, 0L, 1234L),
                new SingleRecord(0, 80, 0L, 1234L),
                new SingleRecord(0, 90, 0L, 1234L)

        }, result);
    }

    @Test
    public void testGetRange_91_100() throws Exception {
        SingleRecord[] result = instance.getRange(91,100);

        assertTrue(result.length == 0);
    }

    @Test
    public void testGetRange_51_101() throws Exception {
        instance.add(new SingleRecord(0, 100L, 0L, 1234L));

        SingleRecord[] result = instance.getRange(51,101);

        assertArrayEquals(new SingleRecord[]{
                new SingleRecord(0, 60, 0L, 1234L),
                new SingleRecord(0, 70, 0L, 1234L),
                new SingleRecord(0, 80, 0L, 1234L),
                new SingleRecord(0, 90, 0L, 1234L),
                new SingleRecord(0, 100, 0L, 1234L)
        }, result);
    }

    @Test
    public void testGetRange_41_71() throws Exception {
        SingleRecord[] result = instance.getRange(41,71);

        assertArrayEquals(new SingleRecord[]{
                new SingleRecord(0, 50, 0L, 1234L),
                new SingleRecord(0, 60, 0L, 1234L),
                new SingleRecord(0, 70, 0L, 1234L),
        }, result);
    }

    @Test
    public void testGetRange_71_100() throws Exception {
        SingleRecord[] result = instance.getRange(71,100);

        assertArrayEquals(new SingleRecord[]{
                new SingleRecord(0, 80, 0L, 1234L),
                new SingleRecord(0, 90, 0L, 1234L)
        }, result);
    }

}