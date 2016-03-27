package wpn.hdri.ss.data2;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 09.11.2015
 */
public class AttributesHelper {
    private final Attribute[] attributes;
    public AttributesHelper(Attribute[] attributes) {
        this.attributes = attributes;
    }

    public String getNameByNdx(int ndx){
        return attributes[ndx].fullName;
    }

    public String getAliasByNdx(int ndx){
        return attributes[ndx].alias;
    }

    public Class<?> getTypeByNdx(int ndx){
        return attributes[ndx].type;
    }

}
