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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Aggregates TINE and Tango client factories
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 27.04.12
 */
public class ClientFactory {
    private static final Logger logger = LoggerFactory.getLogger(ClientFactory.class);

    /**
     * Creates an instance of either {@link TangoClient} or {@link TineClient} or null.
     * <p/>
     * Returning instance depends on which factory will succeed to create a client: Tango will try to create TangoClient
     * and Tine will try to create a TineClient
     *
     * NOTE: TINE devices start with '/'
     *
     * @param deviceUrl a device name to which factories try to connect
     * @return new client (BadClient if neither factory was able to create a client)
     */
    public Client createClient(String deviceUrl) {
        URI uri = URI.create(deviceUrl);

        switch (uri.getScheme()) {
            case "tine":
                return new TineClient(uri);
            case "tango":
                return new TangoClient(uri);
            default:
                throw new IllegalArgumentException("Unknown device uri scheme:" + uri.getScheme());
        }
    }
}
