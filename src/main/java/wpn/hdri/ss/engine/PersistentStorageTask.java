package wpn.hdri.ss.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.data.attribute.Attribute;
import wpn.hdri.ss.data.attribute.AttributeName;
import wpn.hdri.ss.data.attribute.AttributeValue;
import wpn.hdri.ss.data.attribute.SingleAttributeValueView;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Map;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 27.09.13
 */
public class PersistentStorageTask implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(PersistentStorageTask.class);

    private final AttributesManager attributesManager;
    private final long threshold;


    private final Path output;
    private final SingleAttributeValueView valueView = new SingleAttributeValueView();


    public PersistentStorageTask(AttributesManager attributesManager, long threshold, String persistentRoot) {
        this.attributesManager = attributesManager;
        this.threshold = threshold;
        this.output = Paths.get(persistentRoot, "data");
    }

    @Override
    public void run() {
        long totalSize = 0;
        for (Attribute<?> attr : attributesManager.getAllAttributes()) {
            totalSize += attr.size();
        }
        if (totalSize < threshold) return;

        StringBuilder bld = new StringBuilder();
        try (BufferedWriter writer = Files.newBufferedWriter(output, Charset.forName("UTF-8"), StandardOpenOption.CREATE)) {
            for (Map.Entry<AttributeName, Collection<AttributeValue<?>>> entry :
                    attributesManager.takeAllAttributeValues(Timestamp.DEEP_PAST, AttributeFilters.none()).asMap().entrySet()) {

                AttributeName attr = entry.getKey();
                writer.write(attr.getFullName());
                for (AttributeValue<?> value : entry.getValue()) {
                    valueView.toStringArray(value, bld);
                    try {
                        writer.write(bld.toString());
                    } finally {
                        bld.setLength(0);
                    }
                }
            }
            //TODO this may cause some values to be lost if they were added while I/O operation was being performed
            for (Attribute<?> attr : attributesManager.getAllAttributes()) {
                attr.clear();
            }
        } catch (IOException e) {
            LOG.error("Unable to store file.", e);
        }
    }
}
