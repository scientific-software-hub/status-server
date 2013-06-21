package wpn.hdri.ss.data;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 21.06.13
 */
public class AttributeValuesViewTest {
    private final Multimap<AttributeName,AttributeValue<?>> values = LinkedListMultimap.create();
    private Timestamp now;

    @Before
    public void before(){
        now = Timestamp.now();
        values.put(new AttributeName("Test","test-double",null), new AttributeValue<Double>("Test/test-double",null,Value.getInstance(Math.PI), now, now));
        values.put(new AttributeName("Test","test-double",null), new AttributeValue<Double>("Test/test-double",null,Value.getInstance(Math.E), now, now));
        values.put(new AttributeName("Test","test-string",null), new AttributeValue<String>("Test/test-string",null,Value.getInstance("Hi"), now, now));
        values.put(new AttributeName("Test","test-int[]",null), new AttributeValue<int[]>("Test/test-int[]",null,Value.getInstance(new int[]{1,2,3}), now, now));
    }

    @Test
    public void testToJsonString() throws Exception {
        String expected =
                "{" +
                        "'Test/test-double':[{" +
                        "'value':" + ValueFormatters.DOUBLE_FORMATTER.format(Math.PI) + "," +
                        "'read':" + now.toString() + "," +
                        "'write':" + now.toString() +
                        "},{" +
                        "'value':" + ValueFormatters.DOUBLE_FORMATTER.format(Math.E) + "," +
                        "'read':" + now.toString() + "," +
                        "'write':" + now.toString() +
                        "}]," +
                        "'Test/test-string':[{"+
                        "'value':'Hi'," +
                        "'read':" + now.toString() + "," +
                        "'write':" + now.toString() +
                        "}]," +
                        "'Test/test-int[]':[{"+
                        "'value':'1;2;3'," +
                        "'read':" + now.toString() + "," +
                        "'write':" + now.toString() +
                        "}]" +
                        "}";

        AttributeValuesView instance = new AttributeValuesView(values,false);

        String result = instance.toJsonString();

        assertEquals(expected, result);
    }

    @Test
    public void testToStringArray() throws Exception {
        String[] expected = new String[]{
                "Test/test-double\n@" + now.toString() + "[" + ValueFormatters.DOUBLE_FORMATTER.format(Math.PI) + "@" + now.toString() + "]\n" +
                "@" + now.toString() + "[" + ValueFormatters.DOUBLE_FORMATTER.format(Math.E) + "@" + now.toString() + "]\n",
                "Test/test-string\n@" + now.toString() + "[Hi@" + now.toString() + "]\n",
                "Test/test-int[]\n@" + now.toString() + "[1;2;3@" + now.toString() + "]\n",
        };

        AttributeValuesView instance = new AttributeValuesView(values,false);

        String[] result = instance.toStringArray();

        assertArrayEquals(expected,result);
    }
}
