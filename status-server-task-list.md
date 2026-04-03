# Status Server — Task List After Refactor Review

## Context

The current refactoring moved the project in the right direction:

- telemetry collection is now separated from domain event processing
- availability logic has its own analyzer
- domain events can be persisted independently
- Prometheus metrics became more expressive

At the same time, a few important issues remain. Some are correctness bugs, some are architectural gaps, and some are technical debt that will become painful later if left unresolved.

This document lists the next recommended tasks in priority order.

---

## 1. Remove double execution of polling tasks

### Problem

`Engine.start()` currently does two things for each polled attribute:

- schedules the polling task with `scheduleWithFixedDelay(...)`
- also starts the same task immediately via `CompletableFuture.runAsync(task)`

That means the same polling task is executed twice on startup.

### Why it matters

This can lead to:

- duplicate reads at startup
- duplicate telemetry records
- duplicate technical events
- inflated failure counters if the source is unstable
- confusing behavior when debugging availability transitions

### Recommended fix

In `Engine.java`:

- remove `CompletableFuture.runAsync(task)`
- keep only `scheduleWithFixedDelay(..., 0L, ...)`

If an immediate bootstrap read is still desired, implement it explicitly and document why it exists.

### Expected outcome

- one polling lifecycle per attribute
- deterministic startup behavior
- cleaner event stream

---

## 2. Replace heuristic state/status detection with explicit classification

### Problem

`MetricsServer` currently decides whether a string value is a short enum-like `state` or a long human-readable `status` by inspecting the string content.

The current rule is roughly:

- uppercase
- short
- no spaces

### Why it matters

This is fragile and can misclassify legitimate values such as:

- `ON`
- `ALARM_ACK`
- `Fault`
- `MOVING_HOME`
- localized text
- short text statuses

In other words, semantics are inferred from formatting rather than from the actual attribute meaning.

### Recommended fix

Introduce explicit semantic classification for attributes.

Good options:

1. Extend configuration with a semantic kind, for example:
   - `VALUE`
   - `STATE`
   - `STATUS`

2. Or classify by attribute metadata/name in a dedicated mapper:
   - `State` -> `STATE`
   - `Status` -> `STATUS`

3. Keep the classification result attached to the in-memory attribute model and let `MetricsServer` render based on that.

### Expected outcome

- stable metric naming
- no accidental reclassification
- cleaner Grafana dashboards
- lower risk of cardinality issues from text labels

---

## 3. Publish real availability metrics from analyzer state, not from snapshot nullability

### Problem

`MetricsServer` currently emits `_up=0` only when `record.value == null`, otherwise `_up=1`.

This is only a proxy for read success, not actual availability.

### Why it matters

This makes `_up` too optimistic:

- a stale signal may still be reported as up
- an event-driven attribute that stopped changing may still be reported as up
- analyzer state and metrics state can diverge

At the moment there are effectively two availability models:

- one in `AvailabilityAnalyzer`
- one implicit inside `MetricsServer`

### Recommended fix

Expose current analyzer-derived availability state to the metrics layer.

Possible approach:

- maintain a shared `current availability state` store
- let `MetricsServer` emit:
  - `control_system_attribute_up`
  - `control_system_attribute_stale`
  - possibly later `control_system_attribute_down`

based on analyzer state, not on record nullability

### Expected outcome

- metrics reflect actual availability logic
- dashboards become consistent with downtime calculations
- alerting becomes meaningful

---

## 4. Add explicit `stale` metric

### Problem

Freshness is already exposed via `control_system_attribute_age_seconds`, but dashboards still need to infer whether a signal is stale.

### Why it matters

Grafana can display age, but health dashboards are much easier to build if staleness is already materialized as a metric.

### Recommended fix

Emit:

- `control_system_attribute_stale{...} 0|1`

using analyzer state or a clear threshold policy.

### Expected outcome

- easier dashboard coloring
- easier alert rules
- less query logic in Grafana

---

## 5. Restore analyzer state after restart

### Problem

Availability state currently lives only in memory:

- current state
- consecutive failures
- downtime start timestamp

If the process restarts during downtime, analyzer state is lost.

### Why it matters

This affects:

- continuity of open intervals
- correctness of downtime closure
- trustworthiness of billing/SLA reporting

This is especially important now that domain events are already persisted.

### Recommended fix

On startup, restore analyzer state from persistence.

Reasonable options:

1. Load open downtime intervals and last known state from DB
2. Replay recent domain or technical events
3. Maintain a compact `current_state` table and hydrate from it

### Expected outcome

- restart-safe availability tracking
- no broken open intervals
- fewer billing inconsistencies

---

## 6. Revisit stale/down thresholds: counts vs time

### Problem

`staleAfter` and `downAfter` are currently counts of consecutive failures, not durations.

### Why it matters

This means semantics depend on poll interval:

- 3 failures at 100 ms = 300 ms
- 3 failures at 5 s = 15 s

For systems with mixed polling delays, the same threshold values mean very different things.

### Recommended fix

Either:

1. keep the current failure-count model, but document it explicitly in config and docs

or

2. move toward time-based thresholds, for example:
   - stale-after-duration
   - down-after-duration

### Expected outcome

- more predictable availability semantics
- thresholds that are easier to explain to operators and business users

---

## 7. Harden MariaDB sink

### Problem

`MariaDbSink` is currently pragmatic but minimal:

- single JDBC connection
- synchronous writes in the event path
- reconnect only after failures
- no batching
- no obvious idempotency protection

### Why it matters

This is acceptable for low-volume early integration, but it can become a bottleneck or source of data inconsistency later.

### Recommended fix

Short-term improvements:

- add clearer logging around connection resets
- add retry/backoff behavior where appropriate
- consider unique keys or idempotency strategy for interval handling

Medium-term improvements:

- optional buffering or async dispatch
- connection pooling if write volume grows

### Expected outcome

- more resilient persistence
- easier debugging when DB connectivity is unstable

---

## 8. Add service-level metrics

### Problem

Current metrics are mostly per-attribute. The service itself exposes too little summary information.

### Recommended fix

Add metrics such as:

- `status_server_monitored_attributes`
- `status_server_up_attributes`
- `status_server_stale_attributes`
- `status_server_down_attributes`
- optionally `status_server_domain_events_total`

### Expected outcome

- faster operational overview
- better summary panels in Grafana
- easier alerting at service level

---

## 9. Clean up dependency baseline

### Problem

The project now uses Java 21 and virtual threads, but some dependencies and plugins are quite old.

Examples include old testing and build stack pieces.

### Why it matters

This is not the highest-priority runtime problem, but it increases long-term maintenance risk.

### Recommended fix

Review and modernize in a controlled pass:

- JUnit / Mockito
- Maven plugins
- legacy utility dependencies that may no longer be needed

### Expected outcome

- cleaner build
- lower maintenance friction
- fewer surprises when upgrading CI or toolchains

---

## 10. Clarify the target architecture in docs

### Problem

The implementation now contains a meaningful event-driven availability pipeline, but the architecture is not yet clearly documented as such.

### Recommended fix

Document the flow explicitly:

- telemetry events
- technical events
- analyzer
- domain events
- MariaDB sink
- metrics export

Also document the intended roles of:

- `_value`
- `_state`
- `_status`
- `_age_seconds`
- `_up`
- future `_stale`

### Expected outcome

- easier onboarding
- less accidental architectural drift
- clearer discussion with stakeholders

---

## Suggested implementation order

### Immediate
1. Remove double polling startup execution
2. Replace heuristic state/status classification
3. Publish analyzer-based availability metrics
4. Add explicit stale metric

### Short-term
5. Restore analyzer state after restart
6. Clarify threshold semantics
7. Add service-level metrics

### Medium-term
8. Harden MariaDB sink
9. Clean dependency baseline
10. Update architecture docs

---

## Final note

The refactor already improved the project significantly.

The biggest success is that the codebase is no longer just a collector with a metrics endpoint. It now has the beginnings of a proper availability and downtime architecture.

The next step is to make that architecture:

- deterministic
- restart-safe
- explicitly modeled
- consistent between analyzer, metrics, and persistence
