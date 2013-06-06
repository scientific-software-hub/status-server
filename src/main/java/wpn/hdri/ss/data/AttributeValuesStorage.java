package wpn.hdri.ss.data;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import wpn.hdri.collection.Maps;
import wpn.hdri.ss.storage.FileSystemStorage;
import wpn.hdri.ss.storage.Storage;
import wpn.hdri.ss.storage.StorageException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class implements storage of the attribute values. The storage should provide very fast access to the latest value
 * as more frequently requested, relative fast access to last 1M values and persist all values.
 * <p/>
 * Implementation uses {@link AtomicReference} to preserve the latest value, {@link ConcurrentNavigableMap} is used to store
 * 1M records and {@link Storage} is used for persistent values
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.04.13
 */
//TODO integrate some IoC container Google-Guice seems to be a good candidate
public class AttributeValuesStorage<T> {
    public static final long PERSIST_VALUES_THRESHOLD = 100;//1M
    public static final long SAVE_TIMESTAMP_THRESHOLD = 50;//0,5M

    private final AtomicLong valuesCounter = new AtomicLong();

    private final ConcurrentNavigableMap<Timestamp, AttributeValue<T>> inMemValues = Maps.newConcurrentNavigableMap();

    //TODO should be injected from Engine
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    //TODO persistent should be defined in the configuration
    private final Storage persistent = new FileSystemStorage(System.getProperty("user.dir"));

    private final AtomicReference<AttributeValue<T>> lastValue = new AtomicReference<AttributeValue<T>>(null);
    //the following is used to determine when it is time to persist in-mem values
    private final AtomicReference<Timestamp> thresholdTimestamp = new AtomicReference<Timestamp>(null);

    /**
     * Stores a new value in lastValue, previous value (if not null) moves to lastMillion, if lastMillion > 1M moves oldest 500K values to persistent
     * <p/>
     * //     * Stores the value only if it is not null and differs from the previous
     *
     * @param value
     * @return true in case the value was stored, false - otherwise
     */
    public boolean addValue(final AttributeValue<T> value) {
        if (lastValue.get() != null && value.getValue().equals(lastValue.get().getValue()))
//                || value.getValue() == Value.NULL)
            return false;

        long counter = valuesCounter.incrementAndGet();

        lastValue.set(value);

        inMemValues.putIfAbsent(value.getReadTimestamp(), value);

        if (counter % PERSIST_VALUES_THRESHOLD == 0) {//persist old values every time counter hits 1M
            //cut off head map - ~500K values persist them and erase
            exec.submit(new Runnable() {
                @Override
                public void run() {
                    Timestamp threshold = thresholdTimestamp.getAndSet(value.getReadTimestamp());
                    ConcurrentNavigableMap<Timestamp, AttributeValue<T>> head = inMemValues.headMap(threshold);
                    Collection<AttributeValue<T>> values = head.values();
                    persist(values);
                    //head is a view of the map so here underlying inMemValues map is also cleared
                    head.clear();
                }
            });
        } else if (counter % SAVE_TIMESTAMP_THRESHOLD == 0) {//save timestamp each 500K values
            thresholdTimestamp.set(value.getReadTimestamp());
        }

        return true;
    }


    public AttributeValue<T> getLastValue() {
        return lastValue.get();
    }

    public Iterable<AttributeValue<T>> getAllValues() {
        try {
            return Iterables.<AttributeValue<T>>concat(
                    inMemValues.values(),
                    persistent.<AttributeValue<T>>load(
                            lastValue.get().getAttributeFullName(),
                            new AttributeValueFactory<T>()));
        } catch (StorageException e) {
            //TODO log
            return inMemValues.values();
        }
    }

    /**
     * Returns all in memory stored values that are newer than timestamp
     *
     * @param timestamp
     * @return values that are stored in memory
     */
    public Iterable<AttributeValue<T>> getInMemoryValues(Timestamp timestamp) {
        return inMemValues.tailMap(timestamp).values();
    }

    public boolean isEmpty() {
        return valuesCounter.get() == 0;
    }

    //TODO check whether any values are already stored if no - return NULL object
    public AttributeValue<T> floorValue(Timestamp timestamp) {
        Map.Entry<Timestamp, AttributeValue<T>> entry = inMemValues.floorEntry(timestamp);
        if (entry == null)//assume that timestamp is smaller than any stored and therefore it is safe to return first entry
            //TODO persisted values?!
            return inMemValues.firstEntry().getValue();
        else
            return entry.getValue();
    }

    public AttributeValue<T> ceilingValue(Timestamp timestamp) {
        Map.Entry<Timestamp, AttributeValue<T>> entry = inMemValues.ceilingEntry(timestamp);
        if (entry == null)//assume that timestamp is greater than any stored and therefore it is safe to return last value
            return lastValue.get();
        else
            return entry.getValue();
    }

    public void clearInMemoryValues() {
        inMemValues.clear();
        //issue #20 - always preserve last value
        inMemValues.put(lastValue.get().getReadTimestamp(), lastValue.get());
    }

    public void persistInMemoryValues() {
        persist(inMemValues.values());
    }

    private void persist(Iterable<AttributeValue<T>> values) {
        String name = lastValue.get().getReadTimestamp().toString();
        Iterable<String> header = AttributeValue.HEADER;

        List<Iterable<String>> body = Lists.newArrayList();
        for (AttributeValue<T> value : values) {
            body.add(value.getStringValues());
        }

        try {
            persistent.save(name, header, body);
        } catch (StorageException e) {
            //TODO log
            throw new RuntimeException(e);
        }
    }
}
