package wpn.hdri.ss.data;

import com.google.common.collect.Multimap;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 03.06.13
 */
@NotThreadSafe
public class AttributesView {
    private final Multimap<AttributeName, AttributeValue<?>> data;
    private final boolean useAliases;

    public AttributesView(Multimap<AttributeName, AttributeValue<?>> data, boolean useAliases) {
        this.data = data;
        this.useAliases = useAliases;
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

        for (Iterator<Map.Entry<AttributeName, Collection<AttributeValue<?>>>> it = data.asMap().entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<AttributeName, Collection<AttributeValue<?>>> entry = it.next();
            bld.append('\'')
                    .append(resolveAttributeName(entry.getKey()))
                    .append('\'').append(':').append('[');

            for (Iterator<AttributeValue<?>> values = entry.getValue().iterator(); values.hasNext(); ) {
                AttributeValue<?> value = values.next();
                bld.append('{');

                bld.append("'value':").append(value.getValue()).append(',')
                        .append("'readTimestamp':").append(value.getReadTimestamp()).append(',')
                        .append("'writeTimestamp':").append(value.getWriteTimestamp());

                bld.append('}');
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

    public String[] toStringArray() {
        //TODO avoid temporary object creation
        List<String> result = new ArrayList<String>(data.keySet().size());
        StringBuilder bld = new StringBuilder();
        for (Map.Entry<AttributeName, Collection<AttributeValue<?>>> entry : data.asMap().entrySet()) {
            bld.append(resolveAttributeName(entry.getKey())).append('\n');
            for (AttributeValue<?> value : entry.getValue()) {
                bld.append('@').append(value.getReadTimestamp())
                        .append('[').append(value.getValue())
                        .append('@').append(value.getWriteTimestamp())
                        .append("]\n");
            }
        }
        result.add(bld.toString());
        bld.setLength(0);

        return result.toArray(new String[result.size()]);
    }

    private String resolveAttributeName(AttributeName attrName) {
        if (useAliases && attrName.getAlias() != null)
            return attrName.getAlias();
        else
            return attrName.getFullName();
    }
}
