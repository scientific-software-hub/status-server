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

package StatusServer;

import wpn.hdri.ss.data.AttributeValue;

import java.util.Collection;
import java.util.Map;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 08.08.12
 */
public class StatusServerCommandHelper {
    private StatusServerCommandHelper() {
    }

    private static final ThreadLocal<StringBuilder> bldLocal = new ThreadLocal<StringBuilder>();

    public static String[] printValues(Collection<AttributeValue<?>> data) {
        StringBuilder bld = getStringBuilderInstance();
        String[] result = new String[data.size()];

        int ndx = 0;
        for (AttributeValue<?> value : data) {
            bld.append(getAttributeNameView(value)).append('\n');
            bld.append(value.getReadTimestamp()).append('[').append(value.getValue()).append(value.getWriteTimestamp()).append(']');
            result[ndx++] = bld.toString();
            bld.setLength(0);
        }
        bld.setLength(0);
        return result;
    }

    private static String getAttributeNameView(AttributeValue<?> value) {
        if(StatusServerAttribute.USE_ALIAS.<Boolean>toTangoAttribute().getCurrentValue() && value.getAlias() != null)
            return value.getAlias();
        else
            return value.getAttributeFullName();
    }

    private static StringBuilder getStringBuilderInstance() {
        StringBuilder bld = bldLocal.get();
        if (bld == null) {
            bldLocal.set(bld = new StringBuilder());
        }
        return bld;
    }

    public static String attributeToString(Map.Entry<String, Collection<AttributeValue<?>>> entry) {
        StringBuilder bld = getStringBuilderInstance();
        bld.append(entry.getKey()).append('\n');
        for (AttributeValue<?> value : entry.getValue()) {
            bld.append(value.getReadTimestamp())
                    .append('[').append(value.getValue())
                    .append(value.getWriteTimestamp())
                    .append("]\n");
        }
        String result = bld.toString();
        bld.setLength(0);
        return result;
    }
}
