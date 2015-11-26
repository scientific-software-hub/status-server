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

package wpn.hdri.ss.data.attribute;

import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.data.Value;

import javax.annotation.concurrent.Immutable;

/**
 * Immutability of this class really depends on immutability of T
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 06.08.12
 */
@Immutable
public class AttributeValue<T> {
    private final String attributeFullName;
    private final String alias;
    private final Value<T> value;
    //when this value was read by StatusServer - this is the key
    private final Timestamp readTimestamp;
    //when this value was written by a remote server
    private final Timestamp writeTimestamp;
    private final String valueAsString;

    @SuppressWarnings("unchecked")
    AttributeValue(String attributeFullName, String alias, Value<? super T> value, Timestamp readTimestamp, Timestamp writeTimestamp) {
        this.attributeFullName = attributeFullName;
        this.alias = alias;
        this.value = (Value<T>) value;//cast from super
        this.valueAsString = value.toString();
        this.readTimestamp = readTimestamp;
        this.writeTimestamp = writeTimestamp;
    }

    public String getAttributeFullName() {
        return attributeFullName;
    }

    public String getAlias() {
        return alias;
    }

    public Value<T> getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AttributeValue that = (AttributeValue) o;

        if (readTimestamp != null ? !readTimestamp.equals(that.readTimestamp) : that.readTimestamp != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return readTimestamp != null ? readTimestamp.hashCode() : 0;
    }

    /**
     * from StatusServer
     */
    public Timestamp getReadTimestamp() {
        return readTimestamp;
    }

    /**
     * from remote server
     */
    public Timestamp getWriteTimestamp() {
        return writeTimestamp;
    }

    public String getValueAsString() {
        return valueAsString;
    }

    public boolean isNull() {
        return value == Value.NULL;
    }
}
