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

package wpn.hdri.ss.engine;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.*;
import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.configuration.Device;
import wpn.hdri.ss.configuration.DeviceAttribute;
import wpn.hdri.ss.data.*;

import javax.annotation.concurrent.ThreadSafe;
import java.util.*;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 30.04.12
 */
@ThreadSafe
public final class AttributesManager {
    // Thread Locals
    private final ThreadLocal<AttributesSnapshot> snapshotLocal = new ThreadLocal<AttributesSnapshot>() {
        @Override
        protected AttributesSnapshot initialValue() {
            return new AttributesSnapshot();
        }
    };

    private final ThreadLocal<AttributeValues> attributeValuesLocal = new ThreadLocal<AttributeValues>() {
        @Override
        protected AttributeValues initialValue() {
            return new AttributeValues();
        }
    };

    // Different combinations of attributes
    private final Map<Attribute<?>, Client> attributes = Maps.newIdentityHashMap();
    private final Multimap<Method, Attribute<?>> attributesByMethod = HashMultimap.create();
    private final BiMap<String, Attribute<?>> attributesByFullName = HashBiMap.create();
    private final Multimap<String, Attribute<?>> attributesByGroup =
            Multimaps.newListMultimap(new HashMap<String, Collection<Attribute<?>>>(), new Supplier<List<Attribute<?>>>() {
                @Override
                public List<Attribute<?>> get() {
                    return Lists.newArrayList();
                }
            });

    private final Map<String, String> badAttributes = new HashMap<String, String>();

    public Attribute<?> initializeAttribute(DeviceAttribute attr, Device dev, Client devClient, Class<?> attributeClass) {
        Attribute<?> attribute = createAttribute(attr, dev, attributeClass);
        attributes.put(attribute, devClient);
        attributesByMethod.put(attr.getMethod(), attribute);
        attributesByFullName.put(attribute.getFullName(), attribute);
        return attribute;
    }

    /**
     * Returns alive attributes defined by method.
     *
     * @param method filter criteria
     * @return unmodifiable collection
     */
    public Collection<Attribute<?>> getAttributesByMethod(Method method) {
        return Collections2.filter(attributesByMethod.get(method), new Predicate<Attribute<?>>() {
            @Override
            public boolean apply(Attribute<?> input) {
                return !badAttributes.containsKey(input.getFullName());
            }
        });
    }

    /**
     * @param timestamp
     * @param filter
     * @return collection of attributes managed by this manager
     */
    public Multimap<String, AttributeValue<?>> takeAllAttributeValues(Timestamp timestamp, AttributeFilter filter) {
        AttributeValues attributeValues = attributeValuesLocal.get();
        attributeValues.clear();
        attributeValues.update(timestamp, filter);

        return attributeValues.getValues();
    }

    Collection<Attribute<?>> getAttributes() {
        return attributes.keySet();
    }

    public Attribute<?> createAttribute(DeviceAttribute attr, Device dev, Class<?> type) {
        Interpolation interpolation = attr.getInterpolation();
        //consider char as numeric type
        if (Number.class.isAssignableFrom(type) || (type.isPrimitive() && type != boolean.class)) {
            return new NumericAttribute<Number>(dev.getName(), attr.getName(), attr.getAlias(), interpolation, attr.getPrecision());
        } else {
            return new NonNumericAttribute<Object>(dev.getName(), attr.getName(), attr.getAlias(), interpolation);
        }
    }

    /**
     * Marks associated with specified fullName attribute as "bad". This means the attribute won't be accessible through {@link this#getAttributes()}
     * any more.
     *
     * @param fullName fullName of the attribute
     * @param message  reason
     */
    public void reportBadAttribute(String fullName, String message) {
        Attribute<?> attribute = attributesByFullName.get(fullName);
        attributes.remove(attribute);
        badAttributes.put(fullName, message);
    }

    /**
     * Erases all the data from attributes
     */
    public void clear() {
        for (Attribute<?> attribute : attributes.keySet()) {
            attribute.clear();
        }
    }

    /**
     * @param groupName
     * @param attrNames full attribute names
     */
    public void createAttributesGroup(String groupName, final Collection<String> attrNames) {
        attributesByGroup.removeAll(groupName);
        attributesByGroup.putAll(groupName, Iterables.filter(attributesByFullName.values(), new Predicate<Attribute<?>>() {
            @Override
            public boolean apply(Attribute<?> input) {
                return attrNames.contains(input.getFullName()) && !badAttributes.containsKey(input.getFullName());
            }
        }));
    }

    public Collection<Attribute<?>> getAttributesByGroup(String groupName) {
        Collection<Attribute<?>> collection = attributesByGroup.get(groupName);
        if (collection == null) {
            return Collections.emptySet();
        }
        return collection;
    }

    public Collection<AttributeValue<?>> takeSnapshot(Timestamp timestamp, final AttributeFilter filter) {
        AttributesSnapshot snapshot = snapshotLocal.get();
        snapshot.clear();
        snapshot.update(timestamp, filter);

        return snapshot.getValues();
    }

    /**
     * Designed to be thread confinement
     */
    private class AttributesSnapshot {
        private final Set<AttributeValue<?>> values = Sets.newLinkedHashSetWithExpectedSize(attributes.size());

        void clear() {
            values.clear();
        }

        void update(Timestamp timestamp, AttributeFilter filter) {
            for (Attribute<?> attr : attributes.keySet()) {
                if (filter.apply(AttributesManager.this, attr))
                    values.add(attr.getAttributeValue(timestamp));
            }
        }

        Collection<AttributeValue<?>> getValues() {
            return values;
        }
    }

    /**
     * Designed to be thread confinement
     */
    private class AttributeValues {
        private final Multimap<String, AttributeValue<?>> values = LinkedListMultimap.create();

        void clear() {
            values.clear();
        }

        void update(Timestamp timestamp, AttributeFilter filter) {
            for (Attribute<?> attr : attributes.keySet()) {
                if (filter.apply(AttributesManager.this, attr))
                    values.putAll(attr.getFullName(), attr.getAttributeValues(timestamp));
            }
        }

        Multimap<String, AttributeValue<?>> getValues() {
            return values;
        }

    }
}
