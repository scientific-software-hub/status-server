package wpn.hdri.ss.tango;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import fr.esrf.TangoApi.PipeBlob;
import wpn.hdri.ss.data2.SingleRecord;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
* @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
* @since 11.11.2015
*/
public enum OutputType {
    PIPE{
        @Override
        public PipeBlob toType(Iterable<SingleRecord<?>> records, boolean useAliases){
            throw new UnsupportedOperationException("This method is not supported in " + this.getClass());
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
}
