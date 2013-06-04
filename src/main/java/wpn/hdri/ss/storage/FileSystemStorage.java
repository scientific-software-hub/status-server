package wpn.hdri.ss.storage;

import com.google.common.io.Closeables;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.04.13
 */
public class FileSystemStorage implements Storage {
    private final File root;

    private final HeaderParser headerParser = new HeaderParser();
    private final BodyParser bodyParser = new BodyParser();

    public FileSystemStorage(String root) {
        this.root = new File(root);
        if (!this.root.exists()) {
            this.root.mkdirs();
        }
    }

    @Override
    public void save(String dataName, Iterable<String> header, Iterable<Iterable<String>> body) throws StorageException {
        Writer writer = null;
        try {
            File file = new File(root, dataName);
            file.getParentFile().mkdirs();
            writer = new BufferedWriter(new FileWriter(file));

            writer.write(headerParser.makeHeader(header));

            for (Iterable<String> data : body) {
                String values = bodyParser.makeBody(data);

                writer.write(values);
            }
        } catch (IOException e) {
            throw new StorageException(e);
        } finally {
            Closeables.closeQuietly(writer);
        }
    }

    @Override
    public <T> Iterable<T> load(String dataName, TypeFactory<T> factory) throws StorageException {
        BufferedReader rdr = null;
        List<T> result = new ArrayList<T>();
        try {
            rdr = new BufferedReader(new FileReader(new File(this.root, dataName)));


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
