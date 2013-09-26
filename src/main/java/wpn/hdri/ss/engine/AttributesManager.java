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
import wpn.hdri.ss.configuration.DeviceAttribute;
import wpn.hdri.ss.data.Method;
import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.data.attribute.*;

import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.Map.Entry;

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
    private final Map<AttributeName, Class<?>> attributeClasses = Maps.newIdentityHashMap();
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

    private final AttributeFactory factory;

    public AttributesManager(AttributeFactory factory) {
        this.factory = factory;
    }

    public Attribute<?> initializeAttribute(DeviceAttribute attr, String devName, Client devClient, Class<?> attributeClass, boolean isArray, AttributeValuesStorageFactory storageFactory) {
        Attribute<?> attribute = factory.createAttribute(attr.getName(), attr.getAlias(), devName, attr.getInterpolation(), attr.getPrecision(), attributeClass, isArray, storageFactory);
        attributes.put(attribute, devClient);
        attributeClasses.put(attribute.getName(), attributeClass);
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

    public Iterable<Entry<AttributeName, Class<?>>> getAttributeClasses() {
        return attributeClasses.entrySet();
    }

    /**
     * @param timestamp
     * @param filter
     * @return collection of attributes managed by this manager
     */
    public Multimap<AttributeName, AttributeValue<?>> takeAllAttributeValues(Timestamp timestamp, AttributeFilter filter) {
        AttributeValues attributeValues = attributeValuesLocal.get();
        attributeValues.clear();
        attributeValues.update(timestamp, filter);

        return attributeValues.getValues();
    }

    Collection<Attribute<?>> getAttributes() {
        return attributes.keySet();
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
                return attrNames.contains(input.getName().getFullName()) && !badAttributes.containsKey(input.getName().getFullName());
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

    public Multimap<AttributeName, AttributeValue<?>> takeSnapshot(Timestamp timestamp, final AttributeFilter filter) {
        AttributesSnapshot snapshot = snapshotLocal.get();
        snapshot.clear();
        snapshot.update(timestamp, filter);

        return snapshot.getValues();
    }

    public Multimap<AttributeName, AttributeValue<?>> takeLatestSnapshot(AttributeFilter filter) {
        AttributesSnapshot snapshot = snapshotLocal.get();
        snapshot.clear();
        snapshot.update(filter);

        return snapshot.getValues();
    }

    public Attribute<?> getAttribute(String attrName) {
        return attributesByFullName.get(attrName);
    }

    /**
     * Designed to be thread confinement
     */
    private class AttributesSnapshot {
        private final Multimap<AttributeName, AttributeValue<?>> values = HashMultimap.create();

        void clear() {
            values.clear();
        }

        void update(Timestamp timestamp, AttributeFilter filter) {
            for (Attribute<?> attr : attributes.keySet()) {
                if (filter.apply(AttributesManager.this, attr))
                    values.put(attr.getName(), attr.getAttributeValue(timestamp));
            }
        }

        void update(AttributeFilter filter) {
            for (Attribute<?> attr : attributes.keySet()) {
                if (filter.apply(AttributesManager.this, attr))
                    values.put(attr.getName(), attr.getLatestAttributeValue());
            }
        }

        Multimap<AttributeName, AttributeValue<?>> getValues() {
            return values;
        }
    }

    /**
     * Designed to be thread confinement
     * <p/>
     * Implementation is backed with custom Multimap implementation. This implementation stores reference in putAll.
     */
    private class AttributeValues {
        private final Multimap<AttributeName, AttributeValue<?>> values = new Multimap<AttributeName, AttributeValue<?>>() {
            private Map<AttributeName, Collection<AttributeValue<?>>> decorated = Maps.newHashMap();

            @Override
            public int size() {
                return decorated.size();
            }

            @Override
            public boolean isEmpty() {
                return decorated.isEmpty();
            }

            @Override
            public boolean containsKey(Object key) {
                return decorated.containsKey(key);
            }

            @Override
            public boolean containsValue(Object value) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public boolean containsEntry(Object key, Object value) {
                return decorated.containsKey(key) && decorated.get(key).contains(value);
            }

            @Override
            public boolean put(AttributeName key, AttributeValue<?> value) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public boolean remove(Object key, Object value) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public boolean putAll(AttributeName key, Iterable<? extends AttributeValue<?>> values) {
                decorated.put(key, (Collection<AttributeValue<?>>) values);
                return true;
            }

            @Override
            public boolean putAll(Multimap<? extends AttributeName, ? extends AttributeValue<?>> multimap) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Collection<AttributeValue<?>> replaceValues(AttributeName key, Iterable<? extends AttributeValue<?>> values) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Collection<AttributeValue<?>> removeAll(Object key) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void clear() {
                decorated.clear();
            }

            @Override
            public Collection<AttributeValue<?>> get(AttributeName key) {
                return decorated.get(key);
            }

            @Override
            public Set<AttributeName> keySet() {
                return decorated.keySet();
            }

            @Override
            public Multiset<AttributeName> keys() {
                //TODO cache 
                return HashMultiset.create(decorated.keySet());
            }

            @Override
            public Collection<AttributeValue<?>> values() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Collection<Entry<AttributeName, AttributeValue<?>>> entries() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Map<AttributeName, Collection<AttributeValue<?>>> asMap() {
                return decorated;
            }
        };

        void clear() {
            values.clear();
        }

        void update(Timestamp timestamp, AttributeFilter filter) {
            for (Attribute<?> attr : attributes.keySet()) {
                if (filter.apply(AttributesManager.this, attr))
                    values.putAll(attr.getName(), attr.getAttributeValues(timestamp));
            }
        }

        Multimap<AttributeName, AttributeValue<?>> getValues() {
            return values;
        }

    }
}
