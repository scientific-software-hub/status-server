package wpn.hdri.ss.tango;

import com.carrotsearch.hppc.LongArrayList;
import com.carrotsearch.hppc.LongCollection;
import com.carrotsearch.hppc.ObjectArrayList;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import fr.esrf.TangoApi.PipeBlob;
import fr.esrf.TangoApi.PipeBlobBuilder;
import org.tango.utils.ArrayUtils;
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
        public PipeBlob toType(Iterable<SingleRecord<?>> records, boolean useAliases){
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
        public String[] toType(Iterable<SingleRecord<?>> records, boolean useAliases){
            Map<String, StringBuilder> stringBuilderMap = new HashMap<>();

            for(SingleRecord<?> record : records){
                StringBuilder bld = stringBuilderMap.get(record.attribute.fullName);
                if(bld == null) stringBuilderMap.put(record.attribute.fullName,bld = new StringBuilder(record.attribute.fullName));
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
        public String[] toType(Iterable<SingleRecord<?>> records, boolean useAliases){
            throw new UnsupportedOperationException("This method is not supported in " + this.getClass());
        }
    };

    public abstract Object toType(Iterable<SingleRecord<?>> records, boolean useAliases);

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
