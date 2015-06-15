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

import wpn.hdri.ss.data.Interpolation;
import wpn.hdri.ss.data.Timestamp;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 02.05.12
 */
public final class NonNumericAttribute<T> extends Attribute<T> {
    public NonNumericAttribute(String deviceName, String name, String alias, Interpolation interpolation) {
        super(deviceName, name, alias, interpolation);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected boolean addValueInternal(AttributeValue<T> value) {
        return !getAttributeValue().getValue().equals(value.getValue());
    }

    @Override
    public AttributeValue<T> getAttributeValue(Timestamp timestamp, Interpolation interpolation) {
        //Non-numeric does not support linear interpolation
        if (interpolation == Interpolation.LINEAR) {
            return super.getAttributeValue(timestamp, Interpolation.NEAREST);
        } else {
            return super.getAttributeValue(timestamp, interpolation);
        }
    }
}
