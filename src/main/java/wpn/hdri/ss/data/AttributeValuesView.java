package wpn.hdri.ss.data;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;

/**
 * Designed to be thread confinement
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 03.06.13
 */
@NotThreadSafe
public class AttributeValuesView {
    static final Iterable<String> HEADER = Arrays.asList("full_name","alias","type","value","read","write");
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
                bld.append('{');

                bld.append("'value':");
                Class<?> valueClass = value.getValue().get().getClass();
                if(valueClass == String.class || valueClass.isArray())
                    bld.append('\'');
                bld.append(value.getValueAsString());
                if(valueClass == String.class || valueClass.isArray())
                    bld.append('\'');
                bld.append(',')
                        .append("'read':").append(value.getReadTimestamp()).append(',')
                        .append("'write':").append(value.getWriteTimestamp());

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

    private static final ThreadLocal<String[]> LOCAL_RESULT = new ThreadLocal<String[]>();
    public String[] toStringArray() {
        String[] result = LOCAL_RESULT.get();
        int size = values.keySet().size();
        if(result == null || result.length != size){
            LOCAL_RESULT.set(result = new String[size]);
        }

        StringBuilder bld = new StringBuilder();
        int i = 0;
        for (Map.Entry<AttributeName, Collection<AttributeValue<?>>> entry : values.asMap().entrySet()) {
            bld.append(resolveAttributeName(entry.getKey())).append('\n');
            for (AttributeValue<?> value : entry.getValue()) {
                bld.append('@').append(value.getReadTimestamp())
                        .append('[').append(value.getValueAsString())
                        .append('@').append(value.getWriteTimestamp())
                        .append("]\n");
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

    public Iterable<Iterable<String>> toStrings(){
        //TODO avoid temporary object creation
        List<Iterable<String>> result = Lists.newArrayList();
        for(AttributeValue<?> value : values.values()){
            result.add(Arrays.asList(
                    value.getAttributeFullName(),
                    value.getAlias(),
                    value.getValue().get().getClass().getName(),
                    value.getValueAsString(),
                    value.getReadTimestamp().toString(),
                    value.getWriteTimestamp().toString()));
        }
        return result;
    }

}
