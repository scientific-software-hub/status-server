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

package wpn.hdri.ss.client;

import wpn.hdri.ss.client2.ClientAdaptor;
import wpn.hdri.ss.data.Method;

import java.util.EnumMap;

/**
 * Abstract base class for Tango or Tine client.
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 27.04.12
 */
public abstract class Client implements ClientAdaptor {
    protected final EnumMap<Method.EventType, Object> eventTypesMap;
    /**
     * Fully qualified device name. E.g. for Tango: sys/tg_test/1
     */
    private final String deviceName;

    protected Client(String deviceName) {
        this.deviceName = deviceName;
        this.eventTypesMap = mapEventTypes();
    }

    protected abstract EnumMap<Method.EventType, Object> mapEventTypes();

    public String getDeviceName() {
        return deviceName;
    }

    /**
     * Returns appropriate Java type of the attribute's value, i.e. Integer or Long or whatever.
     *
     * @param attrName attribute name
     * @return Class
     * @throws ClientException in case of any error
     */
    public abstract Class<?> getAttributeClass(String attrName) throws ClientException;
}
