# StatusServer

A non-disturbing data collector for the [X-Environment](http://www.github.com/waltz-controls/xenv) (Integrated Control System for High-throughput Tomography). It aggregates control system data from Tango and/or TINE servers, tracks attribute availability, persists downtime intervals to MariaDB, and exposes live metrics via HTTP/Prometheus.

## Requirements

- Java 21
- Maven 3.8+
- Tango 9+ (for Tango attributes) / TINE (for TINE attributes)
- MariaDB 11 (optional — required for downtime persistence)

## Quick Start

```bash
# Build fat JAR
mvn clean package -Dmaven.test.skip=true

# Start MariaDB (and optional Tango stack)
docker compose up -d status-server-db

# Run
java -jar target/status-server-*.jar path/to/config.xml [http-port]
# http-port defaults to 9190
```

## Configuration

Configuration is an XML file. Minimal example:

```xml
<status-server stale-after="3" down-after="6">
  <devices>
    <device name="my/device/1" server="tango://host:10000">
      <attributes>
        <attribute name="Temperature" poll-delay="1000" interpolation="LINEAR"/>
      </attributes>
    </device>
  </devices>
</status-server>
```

Optional MariaDB section (omit to disable persistence):

```xml
<mariadb>
  <jdbc-url>jdbc:mariadb://localhost:3306/statusserver</jdbc-url>
  <user>ss</user>
  <password>ss</password>
</mariadb>
```

| Parameter | Description | Default |
|---|---|---|
| `stale-after` | Consecutive failures before UP→STALE | 3 |
| `down-after` | Consecutive failures before STALE→DOWN | 6 |

## Architecture

```
DeviceSource (XML)
    │
    ▼
EngineFactory ──► Engine
                    │
          ┌─────────┴──────────┐
          ▼                    ▼
     PollTask              EventTask
          │                    │
          ▼                    ▼
  EventSink<SingleRecord<?>>   EventSink<TechnicalEvent>
  (telemetry)                  (AvailabilityAnalyzer)
          │                         │
          ▼                         ▼
   InMemoryWriter         EventSink<DomainEvent>
   (Snapshot only)        (EventDispatcher fan-out)
          │                         │
          ▼                    ┌────┴────┐
   MetricsServer           logger   MariaDbSink
   (HTTP /metrics)
```

### Key components

**`EventSink<T>`** — unified observer interface replacing the old `RecordWriter` and `TechnicalEventListener`. Everything that consumes events implements this single generic interface.

**`EventDispatcher<T>`** — fan-out dispatcher. Calls all registered sinks, isolates failures per sink.

**`AvailabilityAnalyzer`** — consumes `TechnicalEvent`s, maintains per-attribute state machines (UP/STALE/DOWN), emits `DomainEvent`s.

**`AttributeAvailability`** — per-attribute state machine:

```
consecutive failures ≥ stale-after  →  UP    → STALE
consecutive failures ≥ down-after   →  STALE → DOWN  (+ DowntimeOpened)
any success                         →  any   → UP    (+ DowntimeClosed if from DOWN)
```

**`InMemoryWriter`** — snapshot-only in-memory store backing the `/metrics` endpoint.

**`MariaDbSink`** — persists domain events to MariaDB in ERPNext-compatible tables. Reconnects automatically on failure.

## HTTP Endpoints

| Endpoint | Description |
|---|---|
| `GET /metrics` | Prometheus gauge format. Each attribute emits a value gauge and `_up` (1=healthy, 0=failing). |
| `GET /health` | Liveness — always 200. |
| `GET /ready` | Readiness — 503 until engine has started. |

## Availability Tracking & Downtime Persistence

StatusServer classifies each read outcome as a technical event:

| Event | Trigger |
|---|---|
| `ReadSuccess` | Successful attribute read |
| `ReadFailure` | Client exception during read |
| `Timeout` | Read timed out |
| `Disconnect` / `Reconnect` | Connection lost / restored |

The `AvailabilityAnalyzer` aggregates these per attribute and emits domain events when thresholds are crossed:

| Domain Event | Meaning |
|---|---|
| `AvailabilityTransitioned` | State changed (UP↔STALE↔DOWN) |
| `DowntimeOpened` | Attribute entered DOWN state |
| `DowntimeClosed` | Attribute recovered from DOWN |

### MariaDB Schema

Three ERPNext-compatible tables (standard `tab{DocType}` naming, standard audit columns):

```
tabState Transition   — full history of every state change
tabCurrent State      — one row per attribute, UPSERT on every transition
tabDowntime Interval  — one open row per active downtime, closed on recovery
```

Apply schema to a fresh database:

```bash
# Fresh container (wipes existing data)
docker compose down -v
docker compose up -d status-server-db

# Or apply manually to a running container
docker exec -i status-server-db mariadb -u ss -pss statusserver < db/schema.sql
```

On startup, if MariaDB is configured, StatusServer reads `tabCurrent State` and seeds the in-memory state machines — attributes that were DOWN when the server last stopped resume tracking correctly.

## Development

```bash
# Build (skip environment-dependent tests)
mvn clean package -Dmaven.test.skip=true

# Run unit tests (no Tango/TINE required)
mvn test -Dtest="AvailabilityAnalyzerTest,StatusServerStatusServerConfigurationTest"

# Run all tests (requires live Tango)
mvn test
```

Safe unit tests (no external dependencies): `data2/`, `configuration/`, `engine2/AvailabilityAnalyzerTest`.

## Architecture Decision Records

### ADR-1: XML-only device source

Frappe/ERPNext is used as a configuration front-end. It exports device/attribute lists as XML files. StatusServer reads that XML directly. There is no runtime Frappe dependency — this removes the HTTP round-trip on startup and makes the server runnable without an ERPNext instance.

### ADR-2: Unified `EventSink<T>` interface

`RecordWriter` (telemetry) and `TechnicalEventListener` (availability signals) were merged into a single `EventSink<T>` functional interface. Rationale: a writer *is* a listener — it reacts to events. The generic parameter carries the event type, keeping the type system honest while eliminating the duplicate observer hierarchies.

### ADR-3: `EventDispatcher<T>` replaces `WriterDispatcher`

A single generic fan-out dispatcher replaces the telemetry-specific `WriterDispatcher`. Both the telemetry pipeline (`SingleRecord<?>`) and the domain event pipeline (`DomainEvent`) use the same implementation. Failures in one sink are logged and isolated; other sinks always receive the event.

### ADR-4: Snapshot-only in-memory store

`AllRecords` (full time-series history) was retired. The in-memory store now keeps only the latest value per attribute (`Snapshot`). Rationale: historical queries are served by MariaDB; keeping a second growing in-memory copy adds memory pressure with no remaining consumer.

### ADR-5: Global availability thresholds

`stale-after` and `down-after` are global values configured at the server level, not per attribute. Rationale: in this deployment all attributes are polled at similar rates; per-attribute thresholds add configuration complexity without practical benefit.

### ADR-6: ERPNext-compatible MariaDB schema

Tables follow ERPNext naming (`tab{DocType}`) and include the standard Frappe audit columns (`name`, `creation`, `modified`, `modified_by`, `owner`, `docstatus`, `idx`). Rationale: rows can be imported into or consumed by a Frappe/ERPNext instance without transformation, enabling ERP-level downtime reporting and billing workflows.

### ADR-7: No connection pool (plain JDBC with auto-reconnect)

`MariaDbSink` uses a single JDBC connection with `isValid()` check before each use and silent reconnect on failure. Rationale: domain events are low-frequency (state changes, not every poll); a full connection pool (HikariCP etc.) adds a dependency and warm-up complexity for negligible benefit at this throughput.

### ADR-8: Java 21 virtual threads

The HTTP server and engine polling tasks use `Thread.ofVirtual()`. This allows a large number of concurrent blocking I/O operations (Tango/TINE reads) without the overhead of a large platform thread pool.
