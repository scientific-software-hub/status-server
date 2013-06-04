package hzg.wpn.util.compressor;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 04.06.13
 */
public class Compressor {
    private Compressor() {
    }

    /**
     * Encodes bytes into base64 String and then compresses it using standard Deflater
     *
     * @param bytes
     * @return
     */
    public static byte[] encodeAndCompress(byte[] bytes) throws IOException {
        String base64 = Base64.encode(bytes);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Deflater compressor = new Deflater(Deflater.BEST_COMPRESSION, true);
        DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(stream, compressor);
        deflaterOutputStream.write(base64.getBytes());
        deflaterOutputStream.close();

        return stream.toByteArray();
    }

    public static byte[] decompressAndDecode(byte[] bytes) throws IOException, Base64DecodingException {
        ByteArrayOutputStream stream2 = new ByteArrayOutputStream();
        Inflater decompresser = new Inflater(true);
        InflaterOutputStream inflaterOutputStream = new InflaterOutputStream(stream2, decompresser);
        inflaterOutputStream.write(bytes);
        inflaterOutputStream.close();
        byte[] output2 = stream2.toByteArray();

        return Base64.decode(output2);
    }
}
