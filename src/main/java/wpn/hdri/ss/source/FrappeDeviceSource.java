package wpn.hdri.ss.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.configuration.Device;
import wpn.hdri.ss.configuration.DeviceAttribute;
import wpn.hdri.ss.configuration.FrappeConfiguration;
import wpn.hdri.ss.data.Method;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads monitored devices from ERPNext Assets that have {@code monitoring_enabled = 1}.
 *
 * Expected custom fields on the Asset doctype:
 * <ul>
 *   <li>{@code device_url}            – control system URL (tango://, tine://, epics://)</li>
 *   <li>{@code monitored_attributes}  – child table with fields: attribute_name, alias,
 *                                       method (poll|event), delay, interpolation, event_type</li>
 *   <li>{@code monitoring_enabled}    – Check field</li>
 * </ul>
 */
public class FrappeDeviceSource implements DeviceSource {

    private static final Logger logger = LoggerFactory.getLogger(FrappeDeviceSource.class);

    private static final String ASSET_FIELDS =
            "[\"name\",\"device_url\",\"monitored_attributes\"]";
    private static final String ASSET_FILTERS =
            "[[\"monitoring_enabled\",\"=\",1]]";

    private final FrappeConfiguration config;
    private final HttpClient http;
    private final ObjectMapper json;

    public FrappeDeviceSource(FrappeConfiguration config) {
        this.config = config;
        this.http = HttpClient.newHttpClient();
        this.json = new ObjectMapper();
    }

    @Override
    public List<Device> load() throws Exception {
        String url = config.getUrl()
                + "/api/resource/Asset"
                + "?filters=" + URLEncoder.encode(ASSET_FILTERS, StandardCharsets.UTF_8)
                + "&fields=" + URLEncoder.encode(ASSET_FIELDS, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", config.authorizationHeader())
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Frappe API returned HTTP " + response.statusCode()
                    + " for asset list: " + response.body());
        }

        return parseDevices(json.readTree(response.body()));
    }

    private List<Device> parseDevices(JsonNode root) {
        List<Device> devices = new ArrayList<>();
        for (JsonNode assetNode : root.path("data")) {
            String assetName = assetNode.path("name").asText();
            String deviceUrl = assetNode.path("device_url").asText(null);

            if (deviceUrl == null || deviceUrl.isBlank()) {
                logger.warn("Asset '{}' has monitoring_enabled but no device_url — skipping", assetName);
                continue;
            }

            List<DeviceAttribute> attributes = parseAttributes(assetName, assetNode.path("monitored_attributes"));
            if (attributes.isEmpty()) {
                logger.warn("Asset '{}' has no monitored_attributes configured — skipping", assetName);
                continue;
            }

            devices.add(new Device(assetName, deviceUrl, attributes));
            logger.info("Loaded device '{}' ({}) with {} attribute(s)", assetName, deviceUrl, attributes.size());
        }
        return devices;
    }

    private List<DeviceAttribute> parseAttributes(String assetName, JsonNode attrsNode) {
        List<DeviceAttribute> result = new ArrayList<>();
        for (JsonNode node : attrsNode) {
            try {
                DeviceAttribute attr = new DeviceAttribute();
                attr.setName(node.path("attribute_name").asText());
                attr.setAlias(node.path("alias").asText(null));
                attr.setMethod(Method.valueOf(node.path("method").asText("poll").toUpperCase()));
                attr.setDelay(node.path("delay").asLong(3000L));
                attr.setInterpolation(node.path("interpolation").asText("last"));
                attr.setEventType(node.path("event_type").asText(null));
                attr.setPrecision(new BigDecimal(node.path("precision").asText("0")));
                result.add(attr);
            } catch (Exception e) {
                logger.error("Failed to parse monitored_attribute entry for asset '{}': {}", assetName, e.getMessage());
            }
        }
        return result;
    }
}
