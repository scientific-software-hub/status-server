package wpn.hdri.ss.data2;

import wpn.hdri.ss.tango.AttributesGroup;

import java.lang.reflect.Array;
import java.util.Iterator;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 8/18/16
 */
public class FilteredSnapshot implements Iterable<SingleRecord<?>> {
    public final AttributesGroup attributesGroup;
    private final int[] attrPos;
    private final Snapshot snapshot;

    public FilteredSnapshot(AttributesGroup attributesGroup, Snapshot snapshot) {
        this.attributesGroup = attributesGroup;
        this.snapshot = snapshot;

        if(!attributesGroup.isDefault()){
            int groupSize = attributesGroup.size();
            int snapshotSize = Array.getLength(snapshot.getArray());
            this.attrPos = new int[groupSize];

            //TODO attrPos array can be moved to the group and created together with it avoiding this for-loop
            for (int i = 0, j = 0; i < snapshotSize; ++i) {
                SingleRecord<?> record = snapshot.get(i);
                if (attributesGroup.hasAttribute(record.attribute)) attrPos[j++] = i;
            }
        } else {
            attrPos = null;
        }
    }


    public Iterator<SingleRecord<?>> iterator() {
        final Object array = snapshot.getArray();
        final int size = attrPos.length;

        if(attributesGroup.isDefault()) return snapshot.iterator();

        return new Iterator<SingleRecord<?>>() {
            private int pos = 0;

            @Override
            public boolean hasNext() {
                return pos < size;
            }

            @Override
            public SingleRecord<?> next() {
                return snapshot.get(array, attrPos[pos++]);
            }
        };
    }
}
