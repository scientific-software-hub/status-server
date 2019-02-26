package wpn.hdri.ss.tango;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.data2.Attribute;

import java.util.Collection;
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

    public ContextManager(Collection<Attribute<?>> attributes, Collection<String> failedAttributes) {
        this.attributes = attributes;
        this.failedAttributes = failedAttributes;
    }

    public void setClientId(String clientId){
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
}
