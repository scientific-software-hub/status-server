package wpn.hdri.ss.engine2;

import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.configuration.DeviceAttribute;

/**
 * An attribute that could not be initialized at startup (upstream unavailable).
 * Held by the Engine for periodic retry.
 */
public record PendingAttribute(int id, Client client, DeviceAttribute devAttr, String fullName) {}
