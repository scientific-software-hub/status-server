package wpn.hdri.ss.data2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 16.11.2015
 */
public enum Interpolation {
    LAST {
        @Override
        public <T> SingleRecord<T> interpolateInternal(SingleRecord<T> left, SingleRecord<T> right, long t) {
            return left;
        }
    },
    NEAREST {
        @Override
        public <T> SingleRecord<T> interpolateInternal(SingleRecord<T> left, SingleRecord<T> right, long t) {
            if(t - left.r_t <= right.r_t - t) return left;
            else return right;
        }
    },
    LINEAR {
        @Override
        public <T> SingleRecord<T> interpolateInternal(SingleRecord<T> left, SingleRecord<T> right, long t) {
            if(!Number.class.isAssignableFrom(left.value.getClass())){
                logger.warn("Can not interpolate non number classes. Fallback to NEAREST interpolation.");
                return NEAREST.interpolateInternal(left, right, t);
            }

            double v0 = ((Number)left.value).doubleValue();
            double v1 = ((Number)right.value).doubleValue();

            long t0 = left.r_t;
            long t1 = right.r_t;

            double v = v0 + (v1 - v0) * ((t - t0) / (t1 - t0));

            if(left.value.getClass() == short.class)
                return new SingleRecord<>(left.attribute, t, left.w_t, (T)(Short)(short) v);
            else if(left.value.getClass() == int.class)
                return new SingleRecord<>(left.attribute, t, left.w_t, (T)(Integer)(int) v);
            else if(left.value.getClass() == long.class)
                return new SingleRecord<>(left.attribute, t, left.w_t, (T)(Long)(long) v);
            else if(left.value.getClass() == float.class)
                return new SingleRecord<>(left.attribute, t, left.w_t, (T)(Float)(float) v);
            else
                return new SingleRecord<>(left.attribute, t, left.w_t, (T)(Double) v);
        }
    };

    private static final Logger logger = LoggerFactory.getLogger(Interpolation.class);

    public <T> SingleRecord<T> interpolate(SingleRecord<T> left, SingleRecord<T> right, long t){
        return interpolateInternal(left, right, t);
    }

    protected abstract <T> SingleRecord<T> interpolateInternal(SingleRecord<T> left, SingleRecord<T> right, long t);
}
