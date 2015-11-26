package wpn.hdri.ss.data2;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 09.11.2015
 */

public class SingleRecord<T> {
    public final int id;
    public final Attribute<T> attribute; //attr.id
    public final long r_t; //rea_timestamp
    public final long w_t; //write_timestamp
    public final T value; //value bits
    //TODO do we need padding here?

    public SingleRecord(Attribute<T> attribute, long r_t, long w_t, T value) {
        this.attribute = attribute;
        this.id = attribute == null ? 0 : attribute.id;
        this.r_t = r_t;
        this.w_t = w_t;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SingleRecord that = (SingleRecord) o;

        if (r_t != that.r_t) return false;
        if (id != that.id) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (int) (r_t ^ (r_t >>> 32));
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
