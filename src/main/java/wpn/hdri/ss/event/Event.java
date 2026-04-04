package wpn.hdri.ss.event;

import java.time.Instant;

/**
 * Common base for all events flowing through the system.
 *
 * <p>Known subtypes:
 * <ul>
 *   <li>{@link TechnicalEvent} — low-level read/connection signals from collection tasks</li>
 *   <li>{@link DomainEvent}    — business-level availability transitions</li>
 *   <li>{@link wpn.hdri.ss.data2.SingleRecord} — collected telemetry data points</li>
 * </ul>
 */
public interface Event {

    /** Internal attribute id (matches {@link wpn.hdri.ss.data2.Attribute#id}). */
    int attributeId();

    /** Wall-clock time at which the event occurred. */
    Instant timestamp();
}
