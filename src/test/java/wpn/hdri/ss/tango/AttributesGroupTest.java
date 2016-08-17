package wpn.hdri.ss.tango;

import org.junit.Test;
import wpn.hdri.ss.data2.Attribute;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 8/17/16
 */
public class AttributesGroupTest {

    @Test
    public void testToString() throws Exception {
        AttributesGroup instance = new AttributesGroup("test-group", Arrays.<Attribute<?>>asList(new Attribute[]{
                new Attribute(0,null,100, null, null, null, "tango://attr0", "attr0", null),
                new Attribute(1,null,100, null, null, null, "tango://attr1", "attr1", null),
                new Attribute(2,null,100, null, null, null, "tango://attr2", "attr2", null),
                new Attribute(3,null,100, null, null, null, "tango://attr3", "attr3", null)
        }));

        assertEquals(String.format("AttributesGroup@%d[test-group;{attr0,attr1,attr2,attr3,}]", instance.hashCode()), instance.toString());
    }
}