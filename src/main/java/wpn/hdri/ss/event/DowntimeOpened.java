package wpn.hdri.ss.event;

import java.time.Instant;

/** Emitted when an attribute transitions to DOWN — opens a billable downtime interval. */
public record DowntimeOpened(int attributeId, Instant timestamp) implements DomainEvent {}
