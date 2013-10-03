package wpn.hdri.ss.engine;

import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.data.attribute.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 27.09.13
 */
public class PersistentStorageTask implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(PersistentStorageTask.class);

    private final AttributesManager attributesManager;
    private final long threshold;

    private final AtomicReference<Timestamp> lastTimestamp = new AtomicReference<>(Timestamp.now());

    private final Path output;

    public PersistentStorageTask(AttributesManager attributesManager, long threshold, String persistentRoot) throws IOException {
        this.attributesManager = attributesManager;
        this.threshold = threshold;
        Files.createDirectories(Paths.get(persistentRoot));
        this.output = Paths.get(persistentRoot, "data");
    }

    @Override
    public void run() {
        long totalSize = 0;
        for (Attribute<?> attr : attributesManager.getAllAttributes()) {
            totalSize += attr.size();
        }
        if (totalSize < threshold) return;

        Timestamp timestamp = lastTimestamp.get();
        persist();
        //TODO replace with iterator remove
        for (Attribute<?> attr : attributesManager.getAllAttributes()) {
            attr.eraseHead(timestamp);
        }
    }

    public void persist(){
        try{
            Multimap<AttributeName, AttributeValue<?>> values = attributesManager.takeAllAttributeValues(lastTimestamp.getAndSet(Timestamp.now()), AttributeFilters.none());
            AttributeValuesView view = new AttributeValuesView(values);

            Files.write(output, Arrays.asList(view.toStringArray()),Charset.forName("UTF-8"),StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.error("Unable to store data file.", e);
        }
    }
}
