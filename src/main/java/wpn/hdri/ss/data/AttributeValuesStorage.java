package wpn.hdri.ss.data;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import wpn.hdri.collection.Maps;
import wpn.hdri.ss.storage.FileSystemStorage;
import wpn.hdri.ss.storage.Storage;
import wpn.hdri.ss.storage.StorageException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class implements storage of the attribute values. The storage should provide very fast access to the latest value
 * as more frequently requested, relative fast access to last 1M values and persist all values.
 *
 * Implementation uses {@link AtomicReference} to preserve the latest value, {@link ConcurrentNavigableMap} is used to store
 * 1M records and {@link Storage} is used for persistent values
 *
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.04.13
 */
public class AttributeValuesStorage<T> {
    private final AtomicLong valuesCounter = new AtomicLong();

    private final ConcurrentNavigableMap<Timestamp, AttributeValue<T>> inMemValues = Maps.newConcurrentNavigableMap();
    //TODO persistent should be defined in the configuration
    private final Storage persistent = new FileSystemStorage(System.getProperty("user.dir"));

    private final AtomicReference<AttributeValue<T>> lastValue;

    public AttributeValuesStorage(String fullName, String alias) {
        Timestamp now = Timestamp.now();
        this.lastValue = new AtomicReference<AttributeValue<T>>(new AttributeValue<T>(fullName,alias,null, now, now));
    }

    /**
     * Stores a new value in lastValue, previous value (if not null) moves to lastMillion, if lastMillion > 1M moves oldest 500K values to persistent
     *
     * Stores the value only if it is not null and differs from the previous
     *
     * @param value
     * @return true in case the value was stored, false - otherwise
     */
    public boolean addValue(AttributeValue<T> value){
        if(value.getValue().equals(lastValue.get().getValue()) ||
                value.getValue() == Value.NULL) return false;

        valuesCounter.incrementAndGet();

        lastValue.set(value);

        inMemValues.putIfAbsent(value.getReadTimestamp(), value);


        //TODO when counter > 500K move last 500K values to persistent

        return true;
    }


    public AttributeValue<T> getLastValue(){
        return lastValue.get();
    }

    public Iterable<AttributeValue<T>> getAllValues(){
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
     *
     * @return values that are stored in memory
     * @param timestamp
     */
    public Iterable<AttributeValue<T>> getInMemoryValues(Timestamp timestamp){
        return inMemValues.tailMap(timestamp).values();
    }

    public boolean isEmpty(){
        return valuesCounter.get() == 0;
    }

    public AttributeValue<T> floorValue(Timestamp timestamp) {
        AttributeValue<T> result = inMemValues.floorEntry(timestamp).getValue();
        if(result == null){
            //TODO return dummy?!
        }
        return result;
    }

    public AttributeValue<T> ceilingValue(Timestamp timestamp) {
        AttributeValue<T> result = inMemValues.ceilingEntry(timestamp).getValue();
        if(result == null){
            //TODO
        }
        return result;
    }

    public void clearInMemoryValues() {
        inMemValues.clear();
    }

    public void persistInMemoryValues() {
        persist(inMemValues.values());
    }

    private void persist(Iterable<AttributeValue<T>> values){
        String name = lastValue.get().getAttributeFullName();
        Iterable<String> header = AttributeValue.HEADER;

        List<Iterable<String>> body = Lists.newArrayList();
        for(AttributeValue<T> value : values){
            body.add(value.getValues());
        }

        try {
            persistent.save(name,header,body);
        } catch (StorageException e) {
            //TODO log
            throw new RuntimeException(e);
        }
    }
}
