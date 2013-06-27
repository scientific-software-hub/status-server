package hzg.wpn.lang;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 27.06.13
 */
public class StringBuilderHelper {
    private StringBuilderHelper(){}

    /**
     * Appends sequences to the bld using delimiter. This will add ending delimiter.
     *
     * @param bld
     * @param delimiter
     * @param sequences
     * @return
     */
    public static StringBuilder appendUsingDelimiter(StringBuilder bld, CharSequence delimiter, CharSequence... sequences){
        for(CharSequence sequence : sequences){
            bld.append(sequence).append(delimiter);
        }
        return bld;
    }
}
