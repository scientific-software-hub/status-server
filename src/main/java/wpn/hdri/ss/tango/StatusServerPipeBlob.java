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

    public void add(String attrName, Object values, long[] times){
        PipeBlobBuilder bld = new PipeBlobBuilder(attrName);
        bld.add("attributes", attrName);
        bld.add("values", values);
        bld.add("times", times);

        blobBuilder.add(attrName, bld.build());
    }

    public PipeBlob asPipeBlob(){
        return blobBuilder.build();
    }
}
