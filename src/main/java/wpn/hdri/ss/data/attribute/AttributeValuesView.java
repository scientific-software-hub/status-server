package wpn.hdri.ss.data.attribute;

import com.google.common.collect.Multimap;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Designed to be thread confinement
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 03.06.13
 */
@NotThreadSafe
public class AttributeValuesView {
    static final Iterable<String> HEADER = Arrays.asList("full_name", "alias", "type", "value", "read", "write");
    private final SingleAttributeValueView valueView = new SingleAttributeValueView();
    private final Multimap<AttributeName, AttributeValue<?>> values;
    private final boolean useAliases;

    public AttributeValuesView(Multimap<AttributeName, AttributeValue<?>> data, boolean useAliases) {
        this.values = data;
        this.useAliases = useAliases;
    }

    public AttributeValuesView(Multimap<AttributeName, AttributeValue<?>> data) {
        this.values = data;
        this.useAliases = false;
    }

    /**
     * {
     * attr:[
     * {
     * value:
     * readTimestamp:
     * writeTimestamp:
     * },
     * ...
     * ],
     * ...
     * }
     *
     * @return json as shown above
     */
    public String toJsonString() {
        //TODO avoid temporary object creation
        StringBuilder bld = new StringBuilder();

        bld.append('{');

        for (Iterator<Map.Entry<AttributeName, Collection<AttributeValue<?>>>> it = values.asMap().entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<AttributeName, Collection<AttributeValue<?>>> entry = it.next();
            bld.append('\'')
                    .append(resolveAttributeName(entry.getKey()))
                    .append('\'').append(':').append('[');

            for (Iterator<AttributeValue<?>> values = entry.getValue().iterator(); values.hasNext(); ) {
                AttributeValue<?> value = values.next();
                valueView.toJsonString(value, bld);
                if (values.hasNext())
                    bld.append(',');
            }
            bld.append(']');
            if (it.hasNext())
                bld.append(',');
        }
        bld.append('}');

        try {
            return bld.toString();
        } finally {
            bld.setLength(0);
        }
    }

    private static final ThreadLocal<String[]> LOCAL_RESULT = new ThreadLocal<String[]>();

    public String[] toStringArray() {
        String[] result = LOCAL_RESULT.get();
        int size = values.keySet().size();
        if (result == null || result.length != size) {
            LOCAL_RESULT.set(result = new String[size]);
        }

        StringBuilder bld = new StringBuilder();
        int i = 0;
        for (Map.Entry<AttributeName, Collection<AttributeValue<?>>> entry : values.asMap().entrySet()) {
            bld.append(resolveAttributeName(entry.getKey())).append('\n');
            for (AttributeValue<?> value : entry.getValue()) {
                valueView.toStringArray(value, bld);
            }
            result[i++] = bld.toString();
            bld.setLength(0);
        }

        return result;
    }

    private String resolveAttributeName(AttributeName attrName) {
        if (useAliases && attrName.getAlias() != null)
            return attrName.getAlias();
        else
            return attrName.getFullName();
    }
}
