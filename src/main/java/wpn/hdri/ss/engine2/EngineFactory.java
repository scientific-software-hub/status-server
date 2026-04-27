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
    private final List<PendingAttribute> pendingAttributes = new ArrayList<>();

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
                // Pre-assign the ID so the Snapshot slot is always reserved
                int id = attrId++;
                String fullName = client.getDeviceName() + "/" + devAttr.getName();

                Class<?> type;
                try {
                    type = client.getAttributeClass(devAttr.getName());
                } catch (ClientException e) {
                    pendingAttributes.add(new PendingAttribute(id, client, devAttr, fullName, e.getMessage()));
                    continue;
                }

                Method.EventType eventType = Method.EventType.valueOf(devAttr.getEventType().toUpperCase());
                Interpolation interpolation = Interpolation.valueOf(devAttr.getInterpolation().toUpperCase());

                Attribute<?> attr = new Attribute<>(
                        id, (ClientAdaptor) client, devAttr.getDelay(),
                        eventType, type, devAttr.getAlias(),
                        fullName, devAttr.getName(), interpolation);

                logger.debug("Monitoring attribute {}", attr.fullName);

                if (devAttr.getMethod() == Method.POLL) {
                    polledAttributes.add(attr);
                } else {
                    eventDrivenAttributes.add(attr);
                }
            }
        }

        if (!pendingAttributes.isEmpty()) {
            String details = pendingAttributes.stream()
                    .map(p -> p.fullName() + ": " + p.reason())
                    .collect(java.util.stream.Collectors.joining(", ", "[", "]"));
            logger.warn("{} attribute(s) unavailable at startup, retry scheduled: {}",
                    pendingAttributes.size(), details);
        }

        // Virtual threads: one per polling task, blocking I/O does not consume OS threads.
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(
                Math.max(1, polledAttributes.size() + 1), // +1 for the retry task
                Thread.ofVirtual().factory());

        return new Engine(exec, telemetrySink, polledAttributes, eventDrivenAttributes, technicalSink,
                new ArrayList<>(pendingAttributes));
    }

    public List<PendingAttribute> getPendingAttributes() {
        return pendingAttributes;
    }
}
