# Status Server: Downtime Tracking & Billing-Oriented Architecture

## 1. Problem Statement

We have a system that collects real-time data from control systems (e.g., Tango, TINE) and exposes it for monitoring and visualization.

The new requirements extend beyond simple monitoring:

### Core goals:
- Track availability (UP/DOWN/STALE) of devices/attributes
- Detect and compute downtime intervals
- Store historical data for analysis
- Integrate with Frappe/ERPNext
- Support billing based on downtime

### Key challenge:
Downtime is not raw data — it is a derived domain concept that must be:
- deterministic
- explainable
- persistent
- auditable (for billing)

---

## 2. Current State (Status Server)

The current system:
- Reads values from Tango/TINE
- Normalizes them into records
- Writes them via writers
- Keeps data in memory (snapshot + history)
- Exposes /metrics for Prometheus

Limitations:
- No explicit availability model
- No downtime intervals
- No domain-level events
- History not structured for business use

---

## 3. Target Architecture

Technical events → AvailabilityAnalyzer → Domain events → Writers → Storage → ERP

---

## 4. Implementation Steps

### Step 1 — Technical Events
Emit:
- ReadSuccess
- ReadFailure
- Timeout
- Disconnect
- Reconnect

---

### Step 2 — AvailabilityAnalyzer
State machine:

UP → STALE → DOWN  
DOWN → UP  
STALE → UP  

---

### Step 3 — Domain Events
- AvailabilityTransitioned
- DowntimeOpened
- DowntimeClosed

---

### Step 4 — Writer Extension
Generalize writers to support domain events (not only telemetry).

---

### Step 5 — Persistence (Postgres)

Tables:
- current_state
- state_transition
- downtime_interval

---

### Step 6 — Runtime State
Keep only current state in memory. Persist everything important.

---

### Step 7 — Recovery
Restore state from DB on restart.

---

### Step 8 — ERP Integration
ERP consumes downtime intervals and handles:
- classification
- billing
- SLA

---

### Step 9 — Metrics
Prometheus remains for live monitoring only.

---

## 5. Why Not Alternatives

### InfluxDB
Good for time series, bad for business semantics and downtime intervals.

### Prometheus
Not reliable for billing-grade calculations.

### ERP-side computation
Mixes concerns, hard to maintain.

---

## 6. Final Architecture

Sources → Status Server → Analyzer → Postgres → ERP → Billing

---

## 7. Summary

- Add domain layer
- Persist transitions and intervals
- Keep ERP clean
- Use Prometheus only for live metrics

---

## 8. Key Principle

Monitoring produces signals.  
Analyzer produces meaning.  
ERP produces business consequences.
