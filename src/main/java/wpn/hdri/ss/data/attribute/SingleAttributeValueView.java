package wpn.hdri.ss.data.attribute;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 27.09.13
 */
public class SingleAttributeValueView {
    /**
     * <pre>
     *     {
     *         'value':...,
     *         'readTimestamp':...,
     *         'writeTimestamp':...
     *     }
     * </pre>
     *
     * @param bld a StringBuilder to append result
     */
    public void toJsonString(AttributeValue<?> value, StringBuilder bld) {
        bld.append('{');

        bld.append("'value':");
        Class<?> valueClass = value.getValue().getValueClass();
        if (valueClass == String.class || valueClass.isArray())
            bld.append('\'');
        bld.append(value.getValueAsString());
        if (valueClass == String.class || valueClass.isArray())
            bld.append('\'');
        bld.append(',')
                .append("'read':").append(value.getReadTimestamp()).append(',')
                .append("'write':").append(value.getWriteTimestamp());

        bld.append('}');
    }

    public void toStringArray(AttributeValue<?> value, StringBuilder bld) {
        bld.append('@').append(value.getReadTimestamp())
                .append('[').append(value.getValueAsString())
                .append('@').append(value.getWriteTimestamp())
                .append("]\n");
    }
}
