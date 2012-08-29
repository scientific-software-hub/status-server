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

import com.google.common.base.Preconditions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 26.04.12
 */
public enum Interpolation {
    LINEAR("linear") {
        /**
         * Calculates a value between two points using the following formula:
         * y=y0+((x-x0)(y1-y0))/x1-x0, where xi - time, yi - values
         * y=f(t)
         *
         * Usually we are dealing with values of sinusoidal type. So it is pretty safe to use such interpolation.
         *
         * Compares writeTimestamp of the attribute values
         *
         * @param left
         * @param right
         * @param timestamp
         * @return a new AttributeValue, where value interpolated, read and writeTimestamp = timestamp
         * @throws NullPointerException if any of the parameters is null
         * @throws NumberFormatException if actual values are not valid numbers
         */
        @Override
        public <T> AttributeValue<T> interpolate(AttributeValue<T> left, AttributeValue<T> right, Timestamp timestamp) {
            checkPreconditions(left, right, timestamp);

            if (left.getWriteTimestamp().equals(timestamp)) {
                return left;
            }

            if (right.getWriteTimestamp().equals(timestamp)) {
                return right;
            }

            Class<T> valueType = (Class<T>) left.getValue().get().getClass();
            if (!Number.class.isAssignableFrom(valueType)) {
                throw new UnsupportedOperationException("Linear interpolation is not supported for value type: " + valueType);
            } else {
                BigDecimal y0 = new BigDecimal(String.valueOf(left.getValue().get()));
                BigDecimal y1 = new BigDecimal(String.valueOf(right.getValue().get()));

                BigDecimal x0 = new BigDecimal(left.getWriteTimestamp().getValue());
                BigDecimal x1 = new BigDecimal(right.getWriteTimestamp().getValue());

                BigDecimal x = new BigDecimal(timestamp.getValue());

                BigDecimal divisor = x1.subtract(x0);
                BigDecimal result = y0.add(x.subtract(x0).multiply(y1.subtract(y0)).divide(divisor, RoundingMode.HALF_UP));

                Value<T> value = Value.getInstance(convertToType(result, valueType));
                return AttributeHelper.newAttributeValue(left.getAttributeFullName(), value, timestamp, timestamp);
            }
        }

        private <T> T convertToType(BigDecimal bigDecimal, Class<T> type) {
            if (Integer.class.isAssignableFrom(type)) {
                return (T) Integer.valueOf(bigDecimal.intValue());
            } else if (Short.class.isAssignableFrom(type)) {
                return (T) Short.valueOf(bigDecimal.shortValue());
            } else if (Long.class.isAssignableFrom(type)) {
                return (T) Long.valueOf(bigDecimal.longValue());
            } else if (Float.class.isAssignableFrom(type)) {
                return (T) Float.valueOf(bigDecimal.floatValue());
            } else if (Double.class.isAssignableFrom(type)) {
                return (T) Double.valueOf(bigDecimal.doubleValue());
            } else {
                throw new IllegalStateException("Numeric type is expected here.");
            }
        }
    },
    LAST("last") {
        @Override
        public <T> AttributeValue<T> interpolate(AttributeValue<T> left, AttributeValue<T> right, Timestamp timestamp) {
            checkPreconditions(left);
            return left;
        }
    },
    NEAREST("nearest") {
        /**
         * Assume a = ceil, b = floor, c = timestamp. Returns floor if (c - a) >= (b - c) otherwise ceil.
         *
         * Compares write timestamps of the attribute value.
         *
         * @param left
         * @param right
         * @param timestamp
         * @return AttributeValue which writeTimestamp is nearest to specified timestamp
         * @throws NullPointerException if any of the parameters is null
         */
        public <T> AttributeValue<T> interpolate(AttributeValue<T> left, AttributeValue<T> right, Timestamp timestamp) {
            checkPreconditions(left, right, timestamp);

            long leftValue = left.getWriteTimestamp().getValue();
            long rightValue = right.getWriteTimestamp().getValue();

            long timestampValue = timestamp.getValue();

            //difference should be small in most cases therefore it is safe to apply here mixed operation
            if (timestampValue - leftValue >= (rightValue - timestampValue)) {
                return right;
            } else {
                return left;
            }
        }
    };
    private final String alias;

    private Interpolation(String alias) {
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

    private final static Map<String, Interpolation> aliases = new HashMap<String, Interpolation>();

    static {
        for (Interpolation v : Interpolation.values()) {
            aliases.put(v.getAlias(), v);
        }
    }

    /**
     * @param alias alias.
     * @return {@link Interpolation} or null
     */
    public static Interpolation forAlias(String alias) {
        return aliases.get(alias);
    }

    protected abstract <T> AttributeValue<T> interpolate(AttributeValue<T> left, AttributeValue<T> right, Timestamp timestamp);

    protected void checkPreconditions(Object... args) {
        for (Object arg : args) {
            Preconditions.checkNotNull(arg);
        }
    }
}
