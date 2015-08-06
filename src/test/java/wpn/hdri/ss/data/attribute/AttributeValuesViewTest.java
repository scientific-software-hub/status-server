package wpn.hdri.ss.data.attribute;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import fr.esrf.TangoApi.DevicePipe;
import fr.esrf.TangoApi.PipeBlob;
import fr.esrf.TangoApi.PipeScanner;
import org.junit.Before;
import org.junit.Test;
import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.data.Value;
import wpn.hdri.ss.data.ValueFormatters;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 21.06.13
 */
public class AttributeValuesViewTest {
    private final Multimap<AttributeName, AttributeValue<?>> values = LinkedListMultimap.create();
    private Timestamp now;

    @Before
    public void before() {
        now = Timestamp.now();
        values.put(new AttributeName("Test", "test-double", null), new AttributeValue<Double>("Test/test-double", null, Value.getInstance(Math.PI), now, now));
        values.put(new AttributeName("Test", "test-double", null), new AttributeValue<Double>("Test/test-double", null, Value.getInstance(Math.E), now, now));
        values.put(new AttributeName("Test", "test-string", null), new AttributeValue<String>("Test/test-string", null, Value.getInstance("Hi"), now, now));
        values.put(new AttributeName("Test", "test-int[]", null), new AttributeValue<int[]>("Test/test-int[]", null, Value.getInstance(new int[]{1, 2, 3}), now, now));
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
                        "'Test/test-string':[{" +
                        "'value':'Hi'," +
                        "'read':" + now.toString() + "," +
                        "'write':" + now.toString() +
                        "}]," +
                        "'Test/test-int[]':[{" +
                        "'value':'1;2;3'," +
                        "'read':" + now.toString() + "," +
                        "'write':" + now.toString() +
                        "}]" +
                        "}";

        AttributeValuesView instance = new AttributeValuesView(values, false);

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

        AttributeValuesView instance = new AttributeValuesView(values, false);

        String[] result = instance.toStringArray();

        assertArrayEquals(expected, result);
    }

    @Test
    public void testToPipeBlob() throws Exception {
        values.put(new AttributeName("Test", "test-int[]", null), new AttributeValue<int[]>("Test/test-int[]", null, Value.getInstance(new int[]{4, 5, 6}), now, now));

        AttributeValuesView instance = new AttributeValuesView(values, false);

        PipeBlob result = instance.toPipeBlob();

        PipeScanner scanner = new DevicePipe("result", result);

        PipeScanner test_double = scanner.nextScanner();
        assertEquals("Test/test-double", test_double.nextString());
        assertArrayEquals(new double[]{Math.PI, Math.E}, test_double.nextArray(double[].class), 0.1D);
        assertArrayEquals(new long[]{now.getValue(), now.getValue()}, test_double.nextArray(long[].class));

        PipeScanner test_string = scanner.nextScanner();
        assertEquals("Test/test-string", test_string.nextString());
        assertArrayEquals(new String[]{"Hi"}, test_string.nextArray(String[].class));
        assertArrayEquals(new long[]{now.getValue()}, test_string.nextArray(long[].class));

        //TODO arrays are not supported in Pipes?
        PipeScanner test_int_arr = scanner.nextScanner();
        assertEquals("Test/test-int[]", test_int_arr.nextString());
        PipeScanner test_int_arr_values = test_int_arr.nextScanner();
        assertArrayEquals(new int[]{1, 2, 3}, test_int_arr_values.nextArray(int[].class));
        assertArrayEquals(new int[]{4, 5, 6}, test_int_arr_values.nextArray(int[].class));
        assertArrayEquals(new long[]{now.getValue(), now.getValue()}, test_int_arr.nextArray(long[].class));
    }
}
