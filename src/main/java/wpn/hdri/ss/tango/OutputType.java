package wpn.hdri.ss.tango;

import com.carrotsearch.hppc.LongArrayList;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import fr.esrf.TangoApi.PipeBlob;
import wpn.hdri.ss.data.attribute.AttributeValue;
import wpn.hdri.ss.data2.SingleRecord;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.*;

/**
* @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
* @since 11.11.2015
*/
public enum OutputType {
    PIPE{
        @Override
        public PipeBlob toType(int totalNumberOfAttributes, Iterable<SingleRecord<?>> records, boolean useAliases){
            StatusServerPipeBlob result = new StatusServerPipeBlob();

            Map<String, RecordsContainer<?>> tmp = new HashMap<>();

            for(SingleRecord<?> record : records){
                RecordsContainer container = tmp.get(record.attribute.fullName);
                if(container == null) tmp.put(record.attribute.fullName, container = new RecordsContainer<>(record.attribute.fullName));

                container.records.add(record);
            }

            for (RecordsContainer<?> container : tmp.values()) {
                if (container.records.size() == 0) continue;//skip empty values in the pipe

                Class<?> valueType = Iterables.getFirst(container.records, null).value.getClass();

                List<Object> values = new ArrayList<>();
                LongArrayList times = new LongArrayList();

                for(SingleRecord<?> record : container.records){
                    values.add(record.value);
                    times.add(record.w_t);
                }

                result.add(container.attrName, values.toArray((Object[]) Array.newInstance(valueType, values.size())), times.toArray());
            }

            return result.asPipeBlob();
        }
    },
    PLAIN{
        @Override
        public String[] toType(int totalNumberOfAttributes, Iterable<SingleRecord<?>> records, boolean useAliases){
            Map<String, StringBuilder> stringBuilderMap = new HashMap<>();

            for(SingleRecord<?> record : records){
                StringBuilder bld = stringBuilderMap.get(record.attribute.fullName);
                if(bld == null) stringBuilderMap.put(record.attribute.fullName,
                        bld = new StringBuilder(useAliases ? record.attribute.alias : record.attribute.fullName));
                bld
                        .append("\n@").append(record.r_t)
                        .append('[').append(String.valueOf(record.value))
                        .append('@').append(record.w_t).append(']');
            }

            return Iterables.toArray(Iterables.transform(stringBuilderMap.values(), new Function<StringBuilder, String>() {
                @Nullable
                @Override
                public String apply(StringBuilder input) {
                    return input.toString();
                }
            }) ,String.class);
        }
    },
    JSON{
        @Override
        public String[] toType(int totalNumberOfAttributes, Iterable<SingleRecord<?>> records, boolean useAliases){
            String[] result = new String[totalNumberOfAttributes];

            Map<String, StringBuilder> stringBuilderMap = new HashMap<>();
            for (SingleRecord<?> record : records) {
                StringBuilder recordBld = stringBuilderMap.get(record.attribute.fullName);
                if(recordBld == null) stringBuilderMap.put(record.attribute.fullName,
                        recordBld = new StringBuilder());


                recordBld.append('\'')
                        .append(useAliases ? record.attribute.alias : record.attribute.fullName)
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

            for(StringBuilder recordBld : stringBuilderMap.values()){
                bld.append(recordBld);
            }


            return result;
        }
    };

    public abstract Object toType(int totalNumberOfAttributes, Iterable<SingleRecord<?>> records, boolean useAliases);

    private static class RecordsContainer<T> implements Iterable<SingleRecord<T>> {
        private final String attrName;
        private final List<SingleRecord<T>> records;

        public RecordsContainer(String attrName) {
            this.attrName = attrName;
            this.records = new ArrayList<>();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RecordsContainer that = (RecordsContainer) o;

            if (attrName != null ? !attrName.equals(that.attrName) : that.attrName != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return attrName != null ? attrName.hashCode() : 0;
        }

        @Override
        public Iterator<SingleRecord<T>> iterator() {
            return records.iterator();
        }
    }
}
