/*
 * The main contributor to this project is Institute of Materials Research,
 * Helmholtz-Zentrum Geesthacht,
 * Germany.
 *
 * This project is a contribution of the Helmholtz Association Centres and
 * Technische Universitaet Muenchen to the ESS Design Update Phase.
 *
 * The project's funding reference is FKZ05E11CG1.
 *
 * Copyright (c) 2012. Institute of Materials Research,
 * Helmholtz-Zentrum Geesthacht,
 * Germany.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package wpn.hdri.ss.data;

import com.google.common.base.Objects;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Stores values and corresponding read timestamps
 * <p/>
 * Implementation is thread safe but does not guarantee that underlying map won't be changed while reading
 * <p/>
 * This class contains most of the logic linked with storing values. The subclasses specify the way how a value is being added.
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 27.04.12
 */
@ThreadSafe
public abstract class Attribute<T> {
    private final AttributeName name;

    protected final AttributeValuesStorage<T> storage;

    //    protected final ConcurrentNavigableMap<Timestamp, AttributeValue<T>> values = Maps.newConcurrentNavigableMap();
    private final Interpolation interpolation;

    public Attribute(String deviceName, String name, String alias, Interpolation interpolation) {
        this.name = new AttributeName(deviceName, name, alias);
        //TODO get storage root from config
        this.storage = new AttributeValuesStorage<T>(this.name.getFullName(), System.getProperty("user.dir"));

        this.interpolation = interpolation;
    }

    /**
     * Adds value associated with timestamp
     *
     * @param addTimestamp
     * @param value
     * @param readTimestamp
     */
    public void addValue(long addTimestamp, Value<? super T> value, long readTimestamp) {
        addValue(new Timestamp(addTimestamp), value, new Timestamp(readTimestamp));
    }

    /**
     * Implementation not thread safe. But it is very unlikely that two thread will access the same attribute
     * in the same time.
     *
     * @param readTimestamp  when the value was read by StatusServer
     * @param value          value
     * @param writeTimestamp when the value was written on the remote server
     */
    public abstract void addValue(Timestamp readTimestamp, Value<? super T> value, Timestamp writeTimestamp);

    /**
     * Returns the latest stored value.
     * <p/>
     * Performance comparable to O(log n) due to underlying SkipList.
     *
     * @return the latest value
     */
    @SuppressWarnings("unchecked")
    public AttributeValue<T> getAttributeValue() {
        return storage.getLastValue();
    }

    public AttributeValue<T> getAttributeValue(long timestamp) {
        return getAttributeValue(new Timestamp(timestamp), interpolation);
    }

    /**
     * @return latest stored value of the attribute
     */
    public AttributeValue<T> getLatestAttributeValue() {
        return storage.getLastValue();
    }

    public AttributeValue<T> getAttributeValue(Timestamp timestamp) {
        return getAttributeValue(timestamp, interpolation);
    }

    /**
     * Returns a view of all values stored after the timestamp.
     *
     * @param timestamp
     * @return
     */
    public Iterable<AttributeValue<T>> getAttributeValues(final Timestamp timestamp) {
        return storage.getInMemoryValues(timestamp);
    }

    /**
     * Performance comparable to O(log n) due to underlying SkipList.
     *
     * @param timestamp     milliseconds
     * @param interpolation overrides default interpolation
     * @return
     */
    public AttributeValue<T> getAttributeValue(long timestamp, Interpolation interpolation) {
        Timestamp key = new Timestamp(timestamp);
        return getAttributeValue(key, interpolation);
    }

    /**
     * @param timestamp
     * @param interpolation overrides default interpolation
     * @return
     */
    @SuppressWarnings("unchecked")
    public AttributeValue<T> getAttributeValue(Timestamp timestamp, Interpolation interpolation) {
        if (storage.isEmpty()) {
            return storage.getLastValue();
        }
        //TODO this creates new ImmutableEntry this could be avoided because there is only one writter to this but many readers - read is safe
        AttributeValue<T> left = storage.floorValue(timestamp);
        AttributeValue<T> right = storage.ceilingValue(timestamp);

        return interpolation.interpolate(
                left,
                right,
                timestamp);
    }

    public AttributeName getName() {
        return name;
    }

    public String getAlias() {
        return name.getAlias();
    }

    public String getDeviceName() {
        return name.getDeviceName();
    }

    public String getFullName() {
        return name.getFullName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attribute attribute = (Attribute) o;

        if (name != null ? !name.equals(attribute.name) : attribute.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }


    @Override
    public String toString() {
        return name.toString();
    }

    public Iterable<AttributeValue<T>> getAttributeValues(long timestamp) {
        return getAttributeValues(new Timestamp(timestamp));
    }

    Iterable<AttributeValue<T>> getAttributeValues() {
        return getAttributeValues(new Timestamp(0L));
    }

    /**
     * Erases inMem data from this attribute simultaneously inMem data is persisted
     */
    public void clear() {
        storage.persistAndClearInMemoryValues();
    }
}
