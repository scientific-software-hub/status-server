package wpn.hdri.ss.client2;

import wpn.hdri.ss.client.ClientException;
import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.data2.SingleRecord;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 10.11.2015
 */
public interface ClientAdaptor {
    public SingleRecord read(Attribute attr) throws ClientException;
}
