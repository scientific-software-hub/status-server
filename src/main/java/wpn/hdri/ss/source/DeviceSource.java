package wpn.hdri.ss.source;

import wpn.hdri.ss.configuration.Device;

import java.util.List;

/**
 * Provides the list of upstream devices and attributes to monitor.
 * Two implementations: XML (static config) and Frappe (read from ERPNext Assets).
 */
public interface DeviceSource {
    List<Device> load() throws Exception;
}
