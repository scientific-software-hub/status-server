package wpn.hdri.ss.configuration;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

/**
 * Connection settings for a Frappe/ERPNext instance.
 * When present in the configuration, devices are loaded from Frappe Assets
 * and downtime events are written back via the Frappe REST API.
 */
@Root(name = "frappe")
public class FrappeConfiguration {

    @Attribute(name = "url")
    private final String url;

    @Attribute(name = "api-key")
    private final String apiKey;

    @Attribute(name = "api-secret")
    private final String apiSecret;

    public FrappeConfiguration(
            @Attribute(name = "url") String url,
            @Attribute(name = "api-key") String apiKey,
            @Attribute(name = "api-secret") String apiSecret) {
        this.url = url;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public String getUrl() {
        return url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    /** Returns the Authorization header value for Frappe token auth. */
    public String authorizationHeader() {
        return "token " + apiKey + ":" + apiSecret;
    }
}
