package wpn.hdri.ss.data2;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 11.11.2015
 */
public class SingleRecords {
    public static String toString(SingleRecord<?> record, Attribute attribute, boolean useAlias, boolean encode){
        StringBuilder bld = new StringBuilder(useAlias ? attribute.alias : attribute.fullName);
        bld.append("\n@").append(record.r_t).append('[').append(String.valueOf(record.value)).append('@').append(record.w_t).append("]\n");
        return bld.toString();
    }
}
