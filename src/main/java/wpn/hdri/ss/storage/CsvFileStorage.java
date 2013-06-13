package wpn.hdri.ss.storage;

import com.google.common.io.Closeables;
import org.apache.commons.io.FileUtils;

import javax.annotation.concurrent.ThreadSafe;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stores values as semicolon separated text file.
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.04.13
 */
@ThreadSafe
public class CsvFileStorage implements Storage {
    //allow only one thread that accesses file
    private final File root;

    private final HeaderParser headerParser = new HeaderParser();
    private final BodyParser bodyParser = new BodyParser();

    /**
     * @param root
     * @param clearRoot if true will delete root on creation
     */
    public CsvFileStorage(String root, boolean clearRoot) {
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
     * <p/>
     * Implementation swallows any IOExceptions
     *
     * @param dataName
     * @param header
     * @param body
     */
    @Override
    public void save(final String dataName, final Iterable<String> header, final Iterable<Iterable<String>> body) throws StorageException {
        Writer writer = null;
        try {
            File file = new File(root, dataName);
            file.getParentFile().mkdirs();
            writer = new BufferedWriter(new FileWriter(file, true));

            if (file.length() == 0)
                writeHeader(writer, header);

            for (Iterable<String> data : body) {
                writeBody(writer, data);
            }

        } catch (IOException e) {
            throw new StorageException(e);
        } finally {
            Closeables.closeQuietly(writer);
        }
    }

    /**
     * Implementation swallows any IOExceptions
     *
     * @param dataName
     */
    @Override
    public void delete(final String dataName) throws StorageException {
        try {
            File file = new File(root, dataName);
            FileUtils.forceDelete(file);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    private void writeHeader(Writer out, Iterable<String> header) throws IOException {
        out.append('#');
        for (String headerItem : header) {
            out.append(headerItem).append(';');
        }
        out.append('\n');
    }

    private void writeBody(Writer out, Iterable<String> body) throws IOException {
        for (String bodyItem : body) {
            out.append(bodyItem).append(';');
        }
        out.append('\n');
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
        BufferedReader rdr = null;
        List<T> result = new ArrayList<T>();
        try {
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
        } catch (FileNotFoundException e) {
            throw new StorageException(e);
        } catch (IOException e) {
            throw new StorageException(e);
        } finally {
            Closeables.closeQuietly(rdr);
        }
    }

    public static class HeaderParser {
        public Iterable<String> parse(String line) {
            return Arrays.asList(line.split(";"));
        }
    }

    public static class BodyParser {
        public Iterable<String> parse(String line) {
            return Arrays.asList(line.split(";"));
        }
    }
}
