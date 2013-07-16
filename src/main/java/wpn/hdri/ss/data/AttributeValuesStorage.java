package wpn.hdri.ss.data;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import hzg.wpn.collection.Maps;
import org.apache.log4j.Logger;
import wpn.hdri.ss.storage.CsvFileStorage;
import wpn.hdri.ss.storage.SingleThreadStorage;
import wpn.hdri.ss.storage.Storage;
import wpn.hdri.ss.storage.StorageException;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class implements storage of the attribute values. The storage should provide very fast access to the latest value
 * as more frequently requested, relative fast access to last 1M values and persist all values.
 * <p/>
 * Implementation is not intended to perform getAllValues fast as it is bounded by the underlying IO
 * <p/>
 * Implementation uses {@link AtomicReference} to preserve the latest value, {@link ConcurrentNavigableMap} is used to store
 * 1M records and {@link Storage} is used for persistent values
 * <p/>
 * Implementation specification:
 * Concurrent read/write -
 * 1) read pretends and overlaps write
 * a) lastValue - reads either previous value or a new one
 * b) inMemValues - may or may not reflect new changes
 * c) allValues - undefined
 * 2) write pretends and overlaps read
 * a) lastValue - reads either previous value or a new one
 * b) inMemValues - may or may not reflect new changes
 * c) allValues - undefined
 * 3) sequential execution
 * no problems
 * Concurrent read/clear - it is client's responsibility to guarantee that clear is not executed concurrently with RW
 * 1) read pretends and overlaps clear - undefined
 * 2) clear pretends and overlaps read - undefined
 * <p/>
 * Current implementation deletes persisted values only during restart
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.04.13
 */
//TODO integrate some IoC container Google-Guice seems to be a good candidate
public class AttributeValuesStorage<T> {
    public static final Logger LOG = Logger.getLogger(AttributeValuesStorage.class);

    //TODO read from configuration
    public static final long PERSIST_VALUES_THRESHOLD = 1000000;//1M
    public static final long SAVE_TIMESTAMP_THRESHOLD = 500000;//0,5M

    private final AtomicLong valuesCounter = new AtomicLong();

    private final ConcurrentNavigableMap<Timestamp, AttributeValue<T>> inMemValues = Maps.newConcurrentNavigableMap();

    private final Storage persistent;

    private final AtomicReference<AttributeValue<T>> lastValue = new AtomicReference<AttributeValue<T>>(null);
    //the following is used to determine when it is time to persist in-mem values
    private final AtomicReference<Timestamp> thresholdTimestamp = new AtomicReference<Timestamp>(null);

    /**
     * This is used as a data id when interacting with the persistent
     */
    private final String name;
    private final long persistTimestampThreshold;
    private final long updateTimestampThreshold;


    public AttributeValuesStorage(String name, String persistentStorageRoot, long persistTimestampThreshold, long updateTimestampThreshold) {
        this.name = name;
        this.persistent = new SingleThreadStorage(new CsvFileStorage(persistentStorageRoot));
        this.persistTimestampThreshold = persistTimestampThreshold;
        this.updateTimestampThreshold = updateTimestampThreshold;
    }

    public AttributeValuesStorage(String name, String persistentStorageRoot) {
        this(name, persistentStorageRoot, PERSIST_VALUES_THRESHOLD, SAVE_TIMESTAMP_THRESHOLD);
    }

    /**
     * Stores a new value in lastValue, previous value (if not null) moves to lastMillion, if lastMillion > 1M moves oldest 500K values to persistent
     * <p/>
     * //     * Stores the value only if it is not null and differs from the previous
     *
     * @param value
     * @param append indicates whether value will be added to inMem and storage
     * @return true in case the value was stored, false - otherwise
     */
    public boolean addValue(final AttributeValue<T> value, boolean append) {
        if (lastValue.get() != null && value.getValue().equals(lastValue.get().getValue()))
//                || value.getValue() == Value.NULL)
            return false;

        lastValue.set(value);

        if (!append) return true;

        long counter = valuesCounter.incrementAndGet();
        LOG.debug(counter + " collected values so far.");

        inMemValues.put(value.getReadTimestamp(), value);

        if (counter % persistTimestampThreshold == 0) {//persist old values every time counter hits 1M
            LOG.debug("persisting values...");
            //cut off head map - ~500K values persist them and erase
            Timestamp threshold = thresholdTimestamp.getAndSet(value.getReadTimestamp());
            ConcurrentNavigableMap<Timestamp, AttributeValue<T>> head = inMemValues.headMap(threshold);
            Collection<AttributeValue<T>> values = head.values();
            persist(values);
            //head is a view of the map so here underlying inMemValues map is also cleared
            head.clear();
            LOG.debug("done.");
        } else if (counter % updateTimestampThreshold == 0) {//save timestamp each 500K values
            thresholdTimestamp.set(value.getReadTimestamp());
        }

        return true;
    }


    public AttributeValue<T> getLastValue() {
        return lastValue.get();
    }

    public Iterable<AttributeValue<T>> getAllValues() {
        try {
            Iterable<AttributeValue<T>> persisted = persistent.<AttributeValue<T>>load(
                    name,
                    new AttributeValueFactory<T>());
            return Iterables.<AttributeValue<T>>concat(
                    persisted,
                    inMemValues.values());
        } catch (StorageException e) {
            LOG.error("Attempt to load persisted values has failed.", e);
            return inMemValues.values();
        }
    }

    /**
     * Returns all in memory stored values that are newer than timestamp.
     * If provided timestamp is greater than the last's inMem or inMem is empty returns lastValue
     *
     * @param timestamp
     * @return values that are stored in memory
     */
    public Iterable<AttributeValue<T>> getInMemoryValues(Timestamp timestamp) {
        //inMem is empty or timestamp is greater than last inMem
        if (inMemValues.lastEntry() == null || inMemValues.lastEntry().getValue().getReadTimestamp().compareTo(timestamp) == -1)
            //TODO avoid collection creation
            return lastValue.get() == null ? Collections.<AttributeValue<T>>emptyList() : Lists.newArrayList(lastValue.get());
        else
            return inMemValues.tailMap(timestamp).values();
    }

    public boolean isEmpty() {
        return valuesCounter.get() == 0;
    }

    //TODO check whether any values are already stored if no - return NULL object
    public AttributeValue<T> floorValue(Timestamp timestamp) {
        //TODO this creates new ImmutableEntry this could be avoided because there is only one writter to this but many readers - read is safe
        Map.Entry<Timestamp, AttributeValue<T>> entry = inMemValues.floorEntry(timestamp);
        if (entry == null)//assume that timestamp is smaller than any stored and therefore it is safe to return first entry
            //TODO persisted values?!
            return inMemValues.firstEntry().getValue();
        else
            return entry.getValue();
    }

    public AttributeValue<T> ceilingValue(Timestamp timestamp) {
        //TODO this creates new ImmutableEntry this could be avoided because there is only one writter to this but many readers - read is safe
        Map.Entry<Timestamp, AttributeValue<T>> entry = inMemValues.ceilingEntry(timestamp);
        if (entry == null)//assume that timestamp is greater than any stored and therefore it is safe to return last value
            return lastValue.get();
        else
            return entry.getValue();
    }

    public void clearInMemoryValues() {
        inMemValues.clear();
        //issue #20 - always preserve last value
//        inMemValues.put(lastValue.get().getReadTimestamp(), lastValue.get());
    }

    public void persistInMemoryValues() {
        persist(inMemValues.values());
    }

    public void persistAndClearInMemoryValues() {
        persistInMemoryValues();
        clearInMemoryValues();
    }

    private void persist(Iterable<AttributeValue<T>> values) {
        if (Iterables.isEmpty(values)) return;

        Iterable<String> header = AttributeValuesView.HEADER;

        Multimap<AttributeName, AttributeValue<?>> data = LinkedListMultimap.create();
        data.putAll(new AttributeName(name, Iterables.getFirst(values, null).getAlias()), values);

        AttributeValuesView view = new AttributeValuesView(data);

        Iterable<Iterable<String>> body = view.toStrings();

        try {
            persistent.save(name, header, body);
        } catch (StorageException e) {
            LOG.error("Attempt to persist values has failed.", e);
            throw new RuntimeException(e);
        }
    }
}
