package wpn.hdri.ss.tango;

import fr.esrf.TangoApi.PipeBlob;
import fr.esrf.TangoApi.PipeBlobBuilder;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 26.11.2015
 */
//TODO move to a dedicated package
public class StatusServerPipeBlob {
    private final PipeBlobBuilder blobBuilder = new PipeBlobBuilder("status_server");

    {
        blobBuilder.add("append", true);
    }

    private final PipeBlobBuilder dataBuilder = new PipeBlobBuilder("data");

    public void add(String attrName, Object values, long[] times){
        PipeBlobBuilder bld = new PipeBlobBuilder(attrName);
        bld.add("value", values);
        bld.add("time", times);

        dataBuilder.add(attrName, bld.build());
    }

    public PipeBlob asPipeBlob(){
        blobBuilder.add("data", dataBuilder.build());
        return blobBuilder.build();
    }
}
