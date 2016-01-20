package wpn.hdri.ss.tango;

/**
* @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
* @since 11.11.2015
*/
interface StatusServerStatus {
    String IDLE = "IDLE";
    String LIGHT_POLLING = "LIGHT_POLLING";
    String LIGHT_POLLING_AT_FIXED_RATE = "LIGHT_POLLING_AT_FIXED_RATE";
    String HEAVY_DUTY = "HEAVY_DUTY";
}
