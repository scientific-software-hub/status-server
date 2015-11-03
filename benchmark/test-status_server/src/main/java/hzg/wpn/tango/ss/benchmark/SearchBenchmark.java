package hzg.wpn.tango.ss.benchmark;

import org.openjdk.jmh.annotations.*;
import wpn.hdri.ss.configuration.DeviceAttribute;
import wpn.hdri.ss.data.Interpolation;
import wpn.hdri.ss.data.Method;
import wpn.hdri.ss.data.Timestamp;
import wpn.hdri.ss.data.Value;
import wpn.hdri.ss.data.attribute.Attribute;
import wpn.hdri.ss.data.attribute.AttributeFactory;
import wpn.hdri.ss.engine.AttributesManager;
import wpn.hdri.ss.engine.ClientsManager;
import wpn.hdri.ss.engine.Engine;
import wpn.hdri.ss.tango.StatusServer;

import java.math.BigDecimal;
import java.sql.Time;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 29.10.2015
 */
public class SearchBenchmark {

    @State(value = Scope.Benchmark)
    public static class SearchBenchmarkState {
        final AttributesManager attributesManager = new AttributesManager(new AttributeFactory());

        final Engine engine = new Engine(null, attributesManager, 0);

        final long now = System.currentTimeMillis();

        private static final int DATA_SIZE = 100000;//100K

        {
            DeviceAttribute deviceAttribute = new DeviceAttribute();
            deviceAttribute.setName("test_double");
            deviceAttribute.setMethod(Method.POLL);
            deviceAttribute.setInterpolation(Interpolation.NEAREST);
            deviceAttribute.setPrecision(BigDecimal.ZERO);

            Attribute<Double> attr = (Attribute<Double>) attributesManager.initializeAttribute(deviceAttribute, "benchmark/test/0", null, double.class, false);



            for(int i = 0; i < DATA_SIZE ; ++i) {
                Timestamp t = new Timestamp(now + i*(long)(Math.random()*1000));
                attr.addValue(t, Value.getInstance(Math.random()),t);
            }
        }
    }


    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public Object benchmarkLatest(SearchBenchmarkState state) throws Exception {
        return state.engine.getLatestValues("default");
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public Object benchmarkSearch(SearchBenchmarkState state) throws Exception {
        return state.engine.getValues(Timestamp.now(), "default");
    }
}
