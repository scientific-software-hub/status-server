package wpn.hdri.ss.source;

import wpn.hdri.ss.configuration.Device;
import wpn.hdri.ss.configuration.StatusServerConfiguration;

import java.util.List;

/**
 * Loads devices from the static {@code <devices>} block in the XML configuration.
 * Used for testing and standalone deployments without Frappe.
 */
public class XmlDeviceSource implements DeviceSource {

    private final StatusServerConfiguration config;

    public XmlDeviceSource(StatusServerConfiguration config) {
        this.config = config;
    }

    @Override
    public List<Device> load() {
        return config.getDevices();
    }
}
