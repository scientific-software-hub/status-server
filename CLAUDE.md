# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build fat JAR (skip tests for environments without Tango/TINE)
mvn clean package -Dmaven.test.skip=true

# Run all tests (requires Tango environment)
mvn clean test

# Run a single test class
mvn test -Dtest=StatusServerStatusServerConfigurationTest

# Build and push Docker image
mvn deploy
```

Many tests require live Tango or TINE server connections and will fail in standalone environments. The safe unit tests are in `data2/` and `configuration/`.

## Architecture Overview

StatusServer is a non-disturbing data collector for the X-Environment (High-throughput Tomography). It aggregates control system data from Tango/TINE servers, buffers it in-memory, and exposes it via HTTP as Prometheus metrics.

### Data Flow

```
DeviceSource → Configuration → EngineFactory → Engine → RecordWriter → MetricsServer
```

1. **DeviceSource** (`source/`) loads the list of devices and attributes to monitor — either from static XML (`XmlDeviceSource`) or from ERPNext/Frappe assets (`FrappeDeviceSource`).

2. **EngineFactory** (`engine2/`) connects to each device via `ClientFactory`, discovers attribute types, and creates `Engine` with two lists: polled attributes and event-driven attributes.

3. **Engine** (`engine2/`) runs polling tasks (`PollTask`) on a `ScheduledExecutorService` and subscribes to change events (`EventTask`) for event-driven attributes. Uses Java 21 virtual threads for blocking I/O.

4. **RecordWriter** (`writer/`) is a pluggable interface. `InMemoryWriter` maintains both a `Snapshot` (latest value per attribute) and `AllRecords` (full history). `WriterDispatcher` chains multiple writers.

5. **MetricsServer** (`http/`) serves HTTP on port 9090:
   - `GET /metrics` — Prometheus gauge format
   - `GET /health` — liveness (always 200)
   - `GET /ready` — readiness (503 until engine starts)

### Key Data Structures

- **`Snapshot`** (`data2/`) — lock-free latest-value store using `AtomicReferenceArray`. One thread writes per column, no locking needed.
- **`AllRecords`** (`data2/`) — full time-series history with LINEAR and LAST interpolation modes.
- **`Attribute<T>`** (`data2/`) — metadata for a monitored attribute (id, type, poll delay or event type, interpolation method, alias).
- **`SingleRecord<T>`** (`data2/`) — a single collected data point with read and write timestamps.

### Client Abstraction

`ClientFactory` (`client/`) routes device URLs to protocol-specific implementations:
- `tango://` → `TangoClient` (using TangoEZ/TangoUtils)
- `tine://` → `TineClient`

The higher-level `ClientAdaptor` (`client2/`) wraps a `Client` and provides typed `read()` and `subscribe()`/`unsubscribe()` operations used by the engine tasks.

### Configuration

`StatusServerConfiguration` is XML-serialized (Simple XML library). The root config references a list of `Device` objects, each with `DeviceAttribute` entries specifying name, poll delay or event type, and interpolation method.

Test configs live in `src/test/resources/conf/`.

## Entry Point

`wpn.hdri.ss.Main` initializes everything: loads config, builds the writer chain, creates the engine via `EngineFactory`, starts `MetricsServer`, registers a shutdown hook, and calls `engine.start()`.
