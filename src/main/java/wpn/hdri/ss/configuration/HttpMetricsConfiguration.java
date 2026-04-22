package wpn.hdri.ss.configuration;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

/**
 * When present in the config, enables the HTTP metrics endpoint (Prometheus, health, ready).
 */
@Root(name = "http-metrics")
public class HttpMetricsConfiguration {

    @Attribute(name = "port")
    private int port;

    public HttpMetricsConfiguration(@Attribute(name = "port") int port) {
        this.port = port;
    }

    public int getPort() { return port; }
}
