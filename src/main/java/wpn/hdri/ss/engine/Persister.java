package wpn.hdri.ss.engine;

import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.data.attribute.AttributeName;
import wpn.hdri.ss.data.attribute.AttributeValue;
import wpn.hdri.ss.data.attribute.AttributeValuesView;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 25.06.2015
 */
public class Persister implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Persister.class);
    private static final ExecutorService exec = MoreExecutors.getExitingExecutorService(
            (ThreadPoolExecutor) Executors.newFixedThreadPool(1), 1000L, TimeUnit.SECONDS);

    private final Engine engine;
    private final Path output;

    public Persister(Engine engine, Path output) {
        this.engine = engine;
        this.output = output;
    }

    @Override
    public void run() {
        logger.trace("Persister:run:enter");

        Multimap<AttributeName, AttributeValue<?>> values = engine.getAllAttributeValues(null, AttributesManager.DEFAULT_ATTR_GROUP);
        AttributeValuesView view = new AttributeValuesView(values);

        try {
            Files.write(output, Arrays.asList(view.toStringArray()), Charset.forName("UTF-8"), StandardOpenOption.CREATE);
        } catch (IOException e) {
            logger.error("Persister:run has failed!", e);
        }

        logger.trace("Persister:run:exit");
    }

    public void start() {
        exec.submit(this);
    }
}
