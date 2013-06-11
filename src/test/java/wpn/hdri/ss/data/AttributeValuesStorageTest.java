package wpn.hdri.ss.data;

import com.google.common.collect.Iterables;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertTrue;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 11.06.13
 */
public class AttributeValuesStorageTest {
    private static final String TEST_ATTR_FULL_NAME = "test/test_double";
    private static final String TEST_ATTR_STORAGE_ROOT = "target/test-classes/storage";
    private static final long TEST_TIMESTAMP = 1000L;

    @Test
    public void testSaveLoad() {
        AttributeValuesStorage<Double> instance = new AttributeValuesStorage<Double>(TEST_ATTR_FULL_NAME, TEST_ATTR_STORAGE_ROOT, 6, 3);

        List<AttributeValue<Double>> expected = new ArrayList<AttributeValue<Double>>();
        for (int i = 0; i < 14; i++) {
            double rnd = Math.random();
            AttributeValue<Double> value = new AttributeValue<Double>(TEST_ATTR_FULL_NAME, null, Value.getInstance(rnd), new Timestamp(TEST_TIMESTAMP + i * 1000), Timestamp.now());
            instance.addValue(value);
            expected.add(value);
        }

        Iterable<AttributeValue<Double>> result = instance.getAllValues();

        assertTrue(Iterables.elementsEqual(expected, result));
    }
}
