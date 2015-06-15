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

import javax.annotation.concurrent.Immutable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Immutability is guaranteed only in case an immutable type (T) is used.
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 27.04.12
 */
@Immutable
public class Value<T> {
    public static final Value<Object> NULL = new Value<Object>(null);

    private final T v;
    private final String str;

    private Value(T v) {
        this.v = v;
        this.str = String.valueOf(v);
    }

    public T get() {
        return v;
    }

    /**
     * Used in {@link ValueHelper#format(Value)} to speed up toString method
     *
     * @return
     */
    String asString() {
        return str;
    }

    @Override
    public int hashCode() {
        return v.hashCode();
    }

    //TODO#1 guarantee identity of Value and do not perform this comparison
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Value && (v != null && v.equals(((Value) obj).v) || v == null && ((Value) obj).v == null);
    }

    @Override
    public String toString() {
        if (v == null) {
            return ValueFormatters.getNullFormatter().format(null);
        }
        ValueFormatter<T> formatter = ValueFormatters.getFormatter((Class<T>) v.getClass());
        if (formatter != null) {
            return formatter.format(v);
        } else {
            return asString();
        }
    }

    private static final ConcurrentMap<Object, Value> CACHE = new ConcurrentHashMap<Object, Value>();

    /**
     * Creates new or returns cached {@link Value}.
     * <p/>
     * IMPORTANT: T should not be a primitive
     *
     * @param v   data
     * @param <T> usually base type is expected here such as Integer, Double, String etc //TODO what about TINE types?!
     * @return {@link Value}
     */
    @SuppressWarnings("unchecked")
    public static <T> Value<T> getInstance(T v) {
        if (v == null) {
            return (Value<T>) Value.NULL;//cast from Object to any
        }

        if (CACHE.containsKey(v)) {
            return CACHE.get(v);//return any
        } else {
            Value<T> newValue = new Value<T>(v);
            Value<T> oldValue = CACHE.putIfAbsent(v, newValue);//put any
            return oldValue == null ? newValue : oldValue;
        }
    }

    @SuppressWarnings("unchecked")
    public Class<?> getValueClass() {
        if (this == Value.NULL) return String.class;
        return v.getClass();
    }
}
