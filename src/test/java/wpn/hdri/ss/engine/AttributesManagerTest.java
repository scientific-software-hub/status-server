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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Test;
import wpn.hdri.ss.configuration.Device;
import wpn.hdri.ss.configuration.DeviceAttribute;
import wpn.hdri.ss.data.Interpolation;
import wpn.hdri.ss.data.Method;
import wpn.hdri.ss.data.attribute.Attribute;
import wpn.hdri.ss.data.attribute.AttributeFactory;
import wpn.hdri.ss.data.attribute.AttributeValuesStorageFactory;

import java.math.BigDecimal;
import java.util.Collection;

import static junit.framework.Assert.assertTrue;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 17.07.12
 */
public class AttributesManagerTest {

    public static final String TEST_ATTR = "test-attr";

    private final AttributesManager instance = new AttributesManager(new AttributeFactory());
    private final DeviceAttribute attr = new DeviceAttribute();

    {
        attr.setName(TEST_ATTR);
        attr.setAlias(null);
        attr.setMethod(Method.POLL);
        attr.setInterpolation(Interpolation.LAST);
        attr.setDelay(20L);
        attr.setPrecision(BigDecimal.ZERO);
    }

    private final Device dev = new Device("Test", Lists.newArrayList(attr));

    @Test
    public void testAttributesGroup() {
        Attribute<?> initializedAttribute = instance.initializeAttribute(attr, dev.getName(), null, Double.class, false, new AttributeValuesStorageFactory(".", 10, 5));
        instance.createAttributesGroup("group1", Sets.newHashSet(initializedAttribute.getFullName()));

        Collection<Attribute<?>> result = instance.getAttributesByGroup("group1");

        assertTrue(result.size() == 1);
        assertTrue(result.contains(initializedAttribute));
    }
}
