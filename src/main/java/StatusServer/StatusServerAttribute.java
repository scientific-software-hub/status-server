package StatusServer;

import fr.esrf.TangoDs.Attr;
import wpn.hdri.tango.attribute.EnumAttrWriteType;
import wpn.hdri.tango.attribute.TangoAttribute;
import wpn.hdri.tango.data.format.TangoDataFormat;
import wpn.hdri.tango.data.type.ScalarTangoDataTypes;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 09.10.12
 */
public enum StatusServerAttribute {
    USE_ALIAS(new TangoAttribute<Boolean>("isUseAliases", TangoDataFormat.<Boolean>createScalarDataFormat(),
            ScalarTangoDataTypes.BOOLEAN, EnumAttrWriteType.READ_WRITE, null)),
    CURRENT_ACTIVITY(new TangoAttribute<String>("crtActivity", TangoDataFormat.<String>createScalarDataFormat(),
            ScalarTangoDataTypes.STRING, EnumAttrWriteType.READ, null)),
    TIMESTAMP(new TangoAttribute<Long>("timestamp", TangoDataFormat.<Long>createScalarDataFormat(),
            ScalarTangoDataTypes.LONG, EnumAttrWriteType.READ, null)),
    DATA_ENCODED(new TangoAttribute<String>("data_encoded", TangoDataFormat.<String>createScalarDataFormat(),
            ScalarTangoDataTypes.STRING, EnumAttrWriteType.READ, null));

    private final TangoAttribute<?> attribute;

    StatusServerAttribute(TangoAttribute<?> attribute) {
        this.attribute = attribute;
    }

    //ouch... this is not nice
    public <T> TangoAttribute<T> toTangoAttribute() {
        return (TangoAttribute<T>) attribute;
    }

    public Attr toAttr() {
        return attribute.toAttr();
    }
}
