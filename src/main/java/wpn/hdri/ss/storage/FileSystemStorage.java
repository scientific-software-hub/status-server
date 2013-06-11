package wpn.hdri.ss.storage;

import com.google.common.io.Closeables;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * Each instance has its own singleThreadExecutorService that is used for file writing and reading operations.
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.04.13
 */
public class FileSystemStorage implements Storage {
    //allow only one thread that accesses file
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final File root;

    //TODO replace with writer.append
    private final HeaderParser headerParser = new HeaderParser();
    private final BodyParser bodyParser = new BodyParser();

    /**
     * @param root
     * @param clearRoot if true will delete root on creation
     */
    public FileSystemStorage(String root, boolean clearRoot) {
        this.root = new File(root, "data");
        if (!this.root.exists()) {
            this.root.mkdirs();
        } else if (clearRoot) {
            try {
                FileUtils.forceDelete(this.root);
            } catch (IOException e) {
                throw new RuntimeException("Can not delete FileSystemStorage root dir " + this.root.getAbsolutePath(), e);
            }
        }
    }

    /**
     * Async write to the FileSystem
     *
     * @param dataName
     * @param header
     * @param body
     */
    @Override
    public void save(final String dataName, final Iterable<String> header, final Iterable<Iterable<String>> body) {
        exec.submit(new Callable<Void>() {
            @Override
            public Void call() throws IOException {
                Writer writer = null;
                try {
                    File file = new File(root, dataName);
                    file.getParentFile().mkdirs();
                    writer = new BufferedWriter(new FileWriter(file, true));

                    if (file.length() == 0)
                        writer.append(headerParser.makeHeader(header));

                    for (Iterable<String> data : body) {
                        String values = bodyParser.makeBody(data);

                        writer.append(values);
                    }
                    return null;
                } finally {
                    Closeables.closeQuietly(writer);
                }
            }
        });
    }

    /**
     * Sync read from the FileSystem
     *
     * @param dataName
     * @param factory
     * @param <T>
     * @return
     * @throws StorageException
     */
    @Override
    public <T> Iterable<T> load(final String dataName, final TypeFactory<T> factory) throws StorageException {
        Future<Iterable<T>> future = exec.submit(new Callable<Iterable<T>>() {
            @Override
            public Iterable<T> call() throws Exception {
                BufferedReader rdr = null;
                List<T> result = new ArrayList<T>();
                rdr = new BufferedReader(new FileReader(new File(root, dataName)));


                String line;

                //read header
                line = rdr.readLine();
                Iterable<String> header = headerParser.parse(line);

                //read body
                while ((line = rdr.readLine()) != null) {
                    Iterable<String> values = bodyParser.parse(line);

                    result.add(factory.createType(dataName, header, values));
                }

                return result;
            }
        });

        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new StorageException(e);
        } catch (ExecutionException e) {
            throw new StorageException(e);
        }
    }

    public static class HeaderParser {
        public String makeHeader(Iterable<String> header) {
            StringBuilder bld = new StringBuilder();

            bld.append('#');
            for (String val : header) {
                bld.append(val).append(';');
            }
            bld.append('\n');

            return bld.toString();
        }

        public Iterable<String> parse(String line) {
            return Arrays.asList(line.split(";"));
        }
    }

    public static class BodyParser {
        public String makeBody(Iterable<String> values) {
            StringBuilder bld = new StringBuilder();

            for (String val : values) {
                bld.append(val).append(';');
            }
            bld.append('\n');

            return bld.toString();
        }

        public Iterable<String> parse(String line) {
            return Arrays.asList(line.split(";"));
        }
    }
}
