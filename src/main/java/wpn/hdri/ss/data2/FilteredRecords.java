package wpn.hdri.ss.data2;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import wpn.hdri.ss.tango.AttributesGroup;

import java.util.Iterator;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 8/18/16
 */
public class FilteredRecords implements Iterable<SingleRecord<?>> {
    private final AttributesGroup group;
    private final Iterable<SingleRecord<?>> records;

    public FilteredRecords(AttributesGroup group, Iterable<SingleRecord<?>> records) {
        this.group = group;
        this.records = records;
    }

    @Override
    public Iterator<SingleRecord<?>> iterator() {
        if(group.isDefault()) return records.iterator();
        return  Iterables.filter(records, new Predicate<SingleRecord<?>>() {
            @Override
            public boolean apply(SingleRecord<?> input) {
                return group.hasAttribute(input.attribute);
            }
        }).iterator();
    }
}
