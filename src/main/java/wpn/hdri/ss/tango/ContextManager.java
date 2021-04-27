package wpn.hdri.ss.tango;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.data2.Attribute;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 11.11.2015
 */
public class ContextManager {
    private static final Logger logger = LoggerFactory.getLogger(ContextManager.class);

    private final Collection<Attribute<?>> attributes;
    private final ThreadLocal<String> clientId = new ThreadLocal<>();

    private final ConcurrentMap<String, Context> clientContexts = new ConcurrentHashMap<>();

    private Collection<String> failedAttributes;

    private final Map<String, AttributesGroup> groups = new HashMap<>();
    private AttributesGroup attributesGroup;

    public ContextManager(Collection<Attribute<?>> attributes, Collection<String> failedAttributes) {
        this.attributes = attributes;
        this.failedAttributes = failedAttributes;
        this.attributesGroup = new DefaultAttributesGroup(attributes);
        this.groups.put("default", attributesGroup);
    }

    public void setClientId(String clientId) {
        this.clientId.set(clientId);
    }

    public Context getContext(){
        Preconditions.checkNotNull(clientId.get());
        logger.debug("Requesting context for client={}", clientId.get());
        Context context = clientContexts.get(clientId.get());
        if(context == null) context = new Context(clientId.get(), attributes);
        logger.debug("Got context[{}]", context.toString());
        Context oldContext = clientContexts.putIfAbsent(clientId.get(), context);
        return oldContext == null ? context : oldContext;
    }

    public String getClientId() {
        return clientId.get();
    }

    public Iterable<String> getFailedAttributes() {
        return failedAttributes;
    }

    public synchronized void selectGroup(String groupName) {
        if (groups.containsKey(groupName))
            this.attributesGroup = groups.get(groupName);
        else
            throw new IllegalArgumentException("AttributesGroup[" + groupName + "] does not exist!");
    }

    public AttributesGroup getGroup() {
        return this.attributesGroup;
    }

    public synchronized void setGroup(AttributesGroup attributesGroup) {
        groups.put(attributesGroup.name, attributesGroup);
        this.attributesGroup = attributesGroup;
    }

    public String getGroupName() {
        return this.attributesGroup.name;
    }

    public Iterable<String> getGroups() {
        return groups.keySet();
    }
}
