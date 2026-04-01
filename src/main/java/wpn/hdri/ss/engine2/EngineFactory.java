package wpn.hdri.ss.engine2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.client.ClientException;
import wpn.hdri.ss.client.ClientFactory;
import wpn.hdri.ss.client2.ClientAdaptor;
import wpn.hdri.ss.configuration.Device;
import wpn.hdri.ss.configuration.DeviceAttribute;
import wpn.hdri.ss.data.Method;
import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.data2.Interpolation;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.event.EventSink;
import wpn.hdri.ss.event.TechnicalEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 11.11.2015
 */
public class EngineFactory {
    private static final Logger logger = LoggerFactory.getLogger(EngineFactory.class);

    private final List<Device> devices;
    private final EventSink<SingleRecord<?>> telemetrySink;
    private final EventSink<TechnicalEvent> technicalSink;
    private final List<String> failedAttributes = new ArrayList<>();

    public EngineFactory(List<Device> devices, EventSink<SingleRecord<?>> telemetrySink, EventSink<TechnicalEvent> technicalSink) {
        this.devices = devices;
        this.telemetrySink = telemetrySink;
        this.technicalSink = technicalSink;
    }

    public Engine newEngine() {
        int attrId = 0;

        List<Attribute> polledAttributes = new ArrayList<>();
        List<Attribute> eventDrivenAttributes = new ArrayList<>();

        ClientFactory clientFactory = new ClientFactory();
        for (Device dev : devices) {
            Client client = clientFactory.createClient(dev.getUrl());

            for (DeviceAttribute devAttr : dev.getAttributes()) {
                Class<?> type;
                try {
                    type = client.getAttributeClass(devAttr.getName());
                } catch (ClientException e) {
                    logger.warn("Failed to connect to {}/{}: {}", dev.getUrl(), devAttr.getName(), e.getMessage());
                    failedAttributes.add(dev.getUrl() + "/" + devAttr.getName());
                    continue;
                }

                Method.EventType eventType = Method.EventType.valueOf(devAttr.getEventType().toUpperCase());
                Interpolation interpolation = Interpolation.valueOf(devAttr.getInterpolation().toUpperCase());

                Attribute<?> attr = new Attribute<>(
                        attrId++, (ClientAdaptor) client, devAttr.getDelay(),
                        eventType, type, devAttr.getAlias(),
                        client.getDeviceName() + "/" + devAttr.getName(),
                        devAttr.getName(), interpolation);

                logger.debug("Monitoring attribute {}", attr.fullName);

                if (devAttr.getMethod() == Method.POLL) {
                    polledAttributes.add(attr);
                } else {
                    eventDrivenAttributes.add(attr);
                }
            }
        }

        if (!failedAttributes.isEmpty()) {
            logger.warn("{} attribute(s) failed to initialize: {}", failedAttributes.size(), failedAttributes);
        }

        // Virtual threads: one per polling task, blocking I/O does not consume OS threads.
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(
                Math.max(1, polledAttributes.size()),
                Thread.ofVirtual().factory());

        return new Engine(exec, telemetrySink, polledAttributes, eventDrivenAttributes, technicalSink);
    }

    public List<String> getFailedAttributes() {
        return failedAttributes;
    }
}
