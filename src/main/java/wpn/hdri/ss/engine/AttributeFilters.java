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

import wpn.hdri.ss.data.Attribute;
import wpn.hdri.ss.data.Method;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 07.08.12
 */
public class AttributeFilters {
    private final ByGroupAttributeFilter byGroupLocal = new ByGroupAttributeFilter();
    private final ByMethodAttributeFilter byMethodLocal = new ByMethodAttributeFilter();

    private final NoneAttributeFilter none = new NoneAttributeFilter();

    private AttributeFilters() {
    }

    private static final ThreadLocal<AttributeFilters> INSTANCE_LOCAL = new ThreadLocal<AttributeFilters>() {
        @Override
        protected AttributeFilters initialValue() {
            return new AttributeFilters();
        }
    };

    public static AttributeFilter byGroup(final String groupName) {
        AttributeFilters instance = getInstance();

        ByGroupAttributeFilter filter = instance.byGroupLocal;
        filter.groupName = groupName;
        return filter;
    }

    public static AttributeFilter byMethod(final Method method) {
        AttributeFilters instance = getInstance();

        ByMethodAttributeFilter filter = instance.byMethodLocal;
        filter.method = method;
        return filter;
    }

    public static AttributeFilter none() {
        AttributeFilters instance = getInstance();
        return instance.none;
    }

    private static AttributeFilters getInstance() {
        AttributeFilters instance = INSTANCE_LOCAL.get();
        return instance;
    }

    /**
     * Designed to be thread confinement
     */
    private static class ByGroupAttributeFilter implements AttributeFilter {
        private String groupName;

        @Override
        public boolean apply(AttributesManager manager, Attribute<?> attribute) {
            return manager.getAttributesByGroup(groupName).contains(attribute);
        }
    }

    /**
     * Designed to be thread confinement
     */
    private static class ByMethodAttributeFilter implements AttributeFilter {
        private Method method;

        @Override
        public boolean apply(AttributesManager manager, Attribute<?> attribute) {
            return manager.getAttributesByMethod(method).contains(attribute);
        }
    }

    private static class NoneAttributeFilter implements AttributeFilter {
        @Override
        public boolean apply(AttributesManager manager, Attribute<?> attribute) {
            return true;
        }
    }
}
