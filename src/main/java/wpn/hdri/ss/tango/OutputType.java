package wpn.hdri.ss.tango;

import com.carrotsearch.hppc.LongArrayList;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.onjava.lang.DoubleToString;
import fr.esrf.TangoApi.PipeBlob;
import org.apache.commons.lang3.ArrayUtils;
import wpn.hdri.ss.data2.Attribute;
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
        public PipeBlob toType(Iterable<SingleRecord<?>> records, Context ctx){
            Collection<RecordsContainer<?>> tmp = toRecordsContainerCollection(records);

            StatusServerPipeBlob result = new StatusServerPipeBlob();
            for (RecordsContainer<?> container : tmp) {
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
        public String[] toType(Iterable<SingleRecord<?>> records, Context ctx){
            Map<String, StringBuilder> stringBuilderMap = new LinkedHashMap<>();

            for(Attribute<?> attr : ctx.attributesGroup.attributes){
                stringBuilderMap.put(attr.fullName, new StringBuilder(ctx.useAliases ? attr.alias : attr.fullName));
            }

            for(SingleRecord<?> record : records){
                if(record == null) continue;
                StringBuilder bld = stringBuilderMap.get(record.attribute.fullName);
                bld
                        .append("\n@").append(record.r_t)
                        .append('[').append(valueToString(record))
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
        public String[] toType(Iterable<SingleRecord<?>> records, Context ctx){
            Collection<RecordsContainer<?>> tmp = toRecordsContainerCollection(records);

            ArrayList<String> result = new ArrayList<>();

            for (RecordsContainer<?> container : tmp) {
                if(container.isEmpty()) continue;

                StringBuilder recordBld = new StringBuilder("{");

                recordBld
                        .append(String.format("\'name\':\'%s\',\'alias\':\'%s\',\'data\':["
                                , container.attrName, container.attrAlias));


                int size = container.records.size();
                List<String> data = new ArrayList<>();
                for (int i = 0; i < size; ++i) {
                    SingleRecord<?> record = container.records.get(i);

                    data.add(String.format("{\'read\':%d,\'write\':%d,\'value\':%s}",
                            record.r_t, record.w_t, valueToString(record)));
                }

                recordBld.append(Joiner.on(',').join(data));
                recordBld.append("]}");

                result.add(recordBld.toString());
            }

            return result.toArray(new String[result.size()]);
        }
    },
    TSV{
        @Override
        public String[] toType(Iterable<SingleRecord<?>> records, final Context ctx) {
            return Iterables.toArray(Iterables.transform(records, new Function<SingleRecord<?>, String>() {
                @Nullable
                @Override
                public String apply(@Nullable SingleRecord<?> input) {
                    return String.format("%s\t%d\t%s\t%d\n",
                            ctx.useAliases ? input.attribute.alias : input.attribute.fullName,
                            input.r_t, valueToString(input), input.w_t);
                }
            }), String.class);
        }
    };

    private static String valueToString(SingleRecord<?> record) {
        if(record.value.getClass().isArray())
            return ArrayUtils.toString(record.value);
        else if(record.value.getClass() == double.class || record.value.getClass() == Double.class){
            StringBuffer buf = new StringBuffer();
            new DoubleToString().append(buf, (double)(Double)record.value);
            return buf.toString();
        } else if(record.value.getClass() == String.class)
            return "\'" + (String)record.value + "\'";
        else
            return String.valueOf(record.value);
    }

    private static Collection<RecordsContainer<?>> toRecordsContainerCollection(Iterable<SingleRecord<?>> records) {
        Map<String, RecordsContainer<?>> tmp = new HashMap<>();

        for(SingleRecord<?> record : records){
            RecordsContainer container = tmp.get(record.attribute.fullName);
            if(container == null) tmp.put(record.attribute.fullName, container = new RecordsContainer<>(record.attribute.fullName, record.attribute.alias));

            container.records.add(record);
        }
        return tmp.values();
    }

    public abstract Object toType(Iterable<SingleRecord<?>> records, Context ctx);

    private static class RecordsContainer<T> implements Iterable<SingleRecord<T>> {
        private final String attrName;
        private final String attrAlias;
        private final List<SingleRecord<T>> records;

        public RecordsContainer(String attrName, String attrAlias) {
            this.attrName = attrName;
            this.attrAlias = attrAlias;
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

        public boolean isEmpty(){
            return records.size() == 0;
        }
    }
}
