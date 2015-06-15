package wpn.hdri.ss.engine;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 30.11.12
 */
public class ActivitySettings {
    private final long delay;
    private final int initialDelay;

    public ActivitySettings(long delay, int initialDelay) {
        this.delay = delay;
        this.initialDelay = initialDelay;
    }

    public long getDelay(){
        return delay;
    }

    public int getInitialDelay(){
        return initialDelay;
    }
}
