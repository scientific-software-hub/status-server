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
import com.google.common.primitives.Longs;

import javax.annotation.concurrent.Immutable;
import java.util.concurrent.TimeUnit;

/**
 * Encapsulates time value in millis.
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 27.04.12
 */
@Immutable
public class Timestamp implements Comparable<Timestamp> {
    public static final Timestamp DEEP_FUTURE = new Timestamp(Long.MAX_VALUE);
    /**
     * Not so deep actually it is only 1970 :-)
     */
    public static final Timestamp DEEP_PAST = new Timestamp(0L);


    private final long value;
    private final String string;

    /**
     * Creates new {@link Timestamp} instance.
     *
     * @param value time
     * @param unit  time unit of the specified value
     */
    public Timestamp(long value, TimeUnit unit) {
        this.value = (unit == TimeUnit.MILLISECONDS ? value : unit.toMillis(value));
        this.string = String.valueOf(this.value);
    }

    /**
     * Creates new {@link Timestamp} instance.
     *
     * @param value time in milliseconds
     */
    public Timestamp(long value) {
        this(value, TimeUnit.MILLISECONDS);
    }

    public Timestamp() {
        this(System.currentTimeMillis());
    }

    public boolean equals(Object o) {
        return this.getClass().isAssignableFrom(o.getClass()) && Objects.equal(value, ((Timestamp) o).value);
    }

    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return string;
    }

    @Override
    public int compareTo(Timestamp o) {
        return Long.compare(value, o.value);
    }

    public long getValue() {
        return value;
    }

    public long convertTo(TimeUnit unit) {
        return unit.convert(value, TimeUnit.MILLISECONDS);
    }

    public Timestamp subtract(Timestamp timestamp) {
        return new Timestamp(this.value - timestamp.value);
    }

    public static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }

    /**
     * @param s long value as String
     */
    public static Timestamp fromString(String s) {
        return new Timestamp(Long.parseLong(s));
    }

    public Timestamp add(Timestamp timestamp) {
        return new Timestamp(this.value + timestamp.value);
    }
}

