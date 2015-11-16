package wpn.hdri.ss.data2;

import java.util.IdentityHashMap;
import java.util.Map;

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
        private final Map<Class<? extends Number>, Void> supportedClasses = new IdentityHashMap<>();

        {
            supportedClasses.put(short.class, null);
            supportedClasses.put(int.class, null);
            supportedClasses.put(long.class, null);
            supportedClasses.put(float.class, null);
            supportedClasses.put(double.class, null);
        }

        @Override
        public boolean canInterpolate(Class<?> type) {
            return supportedClasses.containsKey(type);
        }

        @Override
        public <T> SingleRecord<T> interpolateInternal(SingleRecord<T> left, SingleRecord<T> right, long t) {
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

    public boolean canInterpolate(Class<?> type){
        return true;
    }

    public <T> SingleRecord<T> interpolate(SingleRecord<T> left, SingleRecord<T> right, long t){
        return interpolateInternal(left, right, t);
    }

    protected abstract <T> SingleRecord<T> interpolateInternal(SingleRecord<T> left, SingleRecord<T> right, long t);
}
