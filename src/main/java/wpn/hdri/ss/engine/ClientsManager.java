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

import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.client.ClientFactory;
import wpn.hdri.ss.engine.exception.ClientInitializationException;

import java.util.*;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 30.04.12
 */
//TODO put a reference to a client into Attribute (do not use Strings to link client and attribute)
//TODO client must have references to its attributes
public /*final*/ class ClientsManager {
    private final ClientFactory factory;

    private final Map<String, Client> clients = new HashMap<String, Client>();

    private final Collection<String> aliveClientNames = new ArrayList<String>();
    private final Map<String, String> deadClientNames = new HashMap<String, String>();

    public ClientsManager(ClientFactory factory) {
        this.factory = factory;
    }

    public Client initializeClient(String name) throws ClientInitializationException {
        Client result = factory.createClient(name);
        if (result == null) {
            throw new ClientInitializationException(name, /*TODO create new exception with all exceptions from .wasExceptions() */null);
        }
        clients.put(name, result);
        aliveClientNames.add(name);
        return result;
    }

    /**
     * May return null if the client is dead or not found.
     *
     * @param name client name
     * @return client
     * @throws IllegalStateException if no client was found
     */
    public Client getClient(String name) {
        Client client = clients.get(name);
        if (client == null) {
            throw new IllegalStateException("Null client is not permitted.");
        }
        return client;
    }

    public void reportBadClient(String name, String cause) {
        aliveClientNames.remove(name);
        clients.remove(name);
        deadClientNames.put(name, cause);
    }

    public Collection<Map.Entry<String, String>> getDeadClients() {
        return Collections.unmodifiableSet(deadClientNames.entrySet());
    }
}
