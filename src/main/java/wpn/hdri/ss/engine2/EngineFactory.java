package wpn.hdri.ss.engine2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.client.ClientException;
import wpn.hdri.ss.client.ClientFactory;
import wpn.hdri.ss.client2.ClientAdaptor;
import wpn.hdri.ss.configuration.Device;
import wpn.hdri.ss.configuration.DeviceAttribute;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.data.Method;
import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.data2.Interpolation;

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

    private final int totalNumberOfAttributes;
    private final StatusServerConfiguration configuration;

    private final List<Attribute<?>> selfAttributes;
    private final List<String> failedAttributes = new ArrayList<>();

    public EngineFactory(List<Attribute<?>> selfAttributes, StatusServerConfiguration configuration) {
        this.selfAttributes = selfAttributes;
        int totalNumberOfAttributes = 0;
        totalNumberOfAttributes += configuration.getStatusServerAttributes().size();
        for(Device dev : configuration.getDevices()){
            totalNumberOfAttributes += dev.getAttributes().size();
        }
        this.totalNumberOfAttributes = totalNumberOfAttributes;
        this.configuration = configuration;
    }

    public Engine newEngine(){
        int actualNumberOfAttributes = 0;

        actualNumberOfAttributes += selfAttributes.size();

        List<Attribute> attributes = new ArrayList<>();
        List<Attribute> polledAttributes = new ArrayList<>();
        List<Attribute> eventDrivenAttributes = new ArrayList<>();

        attributes.addAll(selfAttributes);

        ClientFactory clientFactory = new ClientFactory();
        for(Device dev : configuration.getDevices()){
            Client client = null;
            try {
                client = clientFactory.createClient(dev.getUrl());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                continue;
            }

            for(DeviceAttribute devAttr : dev.getAttributes()){
                Class<?> type = null;
                try {
                    type = client.getAttributeClass(devAttr.getName());
                } catch (ClientException e) {
                    logger.error(e.getMessage(), e);
                    failedAttributes.add(dev.getName() + "/" + devAttr.getName());
                    continue;
                }

                Method.EventType eventType = Method.EventType.valueOf(devAttr.getEventType().toUpperCase());

                Interpolation interpolation = Interpolation.valueOf(devAttr.getInterpolation().toUpperCase());

                Attribute<?> attr = new Attribute<>(
                        actualNumberOfAttributes++, (ClientAdaptor) client, devAttr.getDelay(),
                        eventType, type, devAttr.getAlias(), dev.getName() + "/" + devAttr.getName(), devAttr.getName(), interpolation);
                logger.debug("Monitoring attribute {}", attr.fullName);
                logger.debug(attr.toString());

                attributes.add(attr);

                if(devAttr.getMethod() == Method.POLL){
                    polledAttributes.add(attr);
                } else {
                    eventDrivenAttributes.add(attr);
                }
            }
        }


        if(actualNumberOfAttributes != totalNumberOfAttributes) logger.warn("Actual number of monitored attributes[{}] LT total number [{}]", actualNumberOfAttributes, totalNumberOfAttributes);

        ScheduledExecutorService exec = Executors.newScheduledThreadPool(actualNumberOfAttributes - selfAttributes.size());
        DataStorage storage = new DataStorage(actualNumberOfAttributes);

        return new Engine(exec, storage, polledAttributes, eventDrivenAttributes);
    }

    public List<String> getFailedAttributes(){
        return failedAttributes;
    }
}
