# Status Server ↔ VIO Integration

## Overview

Status Server (SS) writes directly into VIO's MariaDB database. SS has no runtime
dependency on VIO being present — it just writes to whatever DB is configured. VIO
then processes SS's raw intervals into proper `Downtime Event` docs.

---

## What SS writes (3 tables)

SS uses a single JDBC connection and inserts via plain SQL, bypassing Frappe hooks.
All tables follow the Frappe convention (`tab{DocType Name}`, standard audit columns).

### `tabCurrent State`
Latest availability state per monitored attribute. Used by SS to restore its internal
state on restart. Should become a VIO Doctype (`Current State`) — SS will still write
to it via JDBC.

| Column | Type | Notes |
|---|---|---|
| `name` | varchar(140) PK | `"CS-{attribute_id}"` |
| `attribute_id` | int UNIQUE | SS internal integer ID |
| `attribute_name` | varchar(255) | e.g. `tango://host/sys/dev/1/Temperature` |
| `state` | varchar(10) | `UP`, `STALE`, or `DOWN` |
| `since` | datetime(6) | When this state began |
| + standard Frappe audit columns | | `creation`, `modified`, `modified_by`=`'status-server'`, `owner`=`'status-server'`, `docstatus`=1, `idx`=0 |

**SS operation:** UPSERT on every availability state transition.

---

### `tabState Transition`
Audit trail of every state change. Should become a VIO Doctype (`State Transition`).

| Column | Type | Notes |
|---|---|---|
| `name` | varchar(140) PK | UUID |
| `attribute_id` | int | SS internal integer ID |
| `attribute_name` | varchar(255) | Full device/attribute path |
| `from_state` | varchar(10) | `UP`, `STALE`, or `DOWN` |
| `to_state` | varchar(10) | `UP`, `STALE`, or `DOWN` |
| `transitioned_at` | datetime(6) | Event timestamp |
| + standard Frappe audit columns | | same as above |

**SS operation:** INSERT on every state transition.

---

### `tabDowntime Interval`
Raw per-attribute downtime intervals. **Not** a VIO Doctype — SS owns it.
VIO reads from it and converts closed intervals into `Downtime Event` docs.

| Column | Type | Notes |
|---|---|---|
| `name` | varchar(140) PK | UUID |
| `attribute_id` | int | SS internal integer ID |
| `attribute_name` | varchar(255) | Full device/attribute path |
| `opened_at` | datetime(6) | When attribute went DOWN |
| `closed_at` | datetime(6) NULL | When it recovered; NULL = still open |
| `duration_seconds` | decimal(15,3) NULL | Computed by SS on close |
| `cause` | varchar(20) | Always `'instrument'` (hardcoded by SS) |
| `processed` | tinyint(1) | `0` = not yet converted by VIO; `1` = done |
| + standard Frappe audit columns | | same as above |

**SS operation:** INSERT on `DowntimeOpened`; UPDATE `closed_at`, `duration_seconds`
on `DowntimeClosed`. `cause` is always `'instrument'`.

---

## What VIO must do

Implement `vio/integrations/status_server.py :: poll_if_configured()` (Frappe
scheduler, every minute). Algorithm:

```
for each row in tabDowntime Interval
    where closed_at IS NOT NULL AND processed = 0:

    find Experiment Session where
        actual_start <= opened_at
        AND (actual_end IS NULL OR actual_end >= opened_at)

    if no session found → skip (downtime outside any session, not billable)

    create Downtime Event:
        session        = <found session name>
        source         = "status_server"
        cause          = row.cause           # "instrument"
        start_time     = row.opened_at
        end_time       = row.closed_at
        deducted_from_billing = 1

    UPDATE tabDowntime Interval SET processed = 1 WHERE name = row.name
```

After inserting the `Downtime Event`, Frappe's existing hooks fire automatically
(`recalculate_session_billing` → `calculate_billable_hours`).

---

## Doctype requirements for VIO

### `Current State` (new Doctype)
- **Fields** (names must match the table columns SS writes exactly):
  `attribute_id` (Int), `attribute_name` (Data), `state` (Select: UP/STALE/DOWN),
  `since` (Datetime)
- **Naming:** set `name` to `"CS-{attribute_id}"` — SS already uses this format.
  Use autoname = `"CS-{attribute_id}"` with a custom naming method, or accept
  that SS sets `name` directly on insert.
- **No submit workflow needed** — SS writes `docstatus=1` directly.
- **Read-only in UI** — SS is the only writer.

### `State Transition` (new Doctype)
- **Fields:** `attribute_id` (Int), `attribute_name` (Data),
  `from_state` (Select: UP/STALE/DOWN), `to_state` (Select: UP/STALE/DOWN),
  `transitioned_at` (Datetime)
- **Naming:** UUID — SS sets `name` directly on insert.
- **Immutable** — append-only, no updates.

---

## Schema creation

SS ships a `db/schema.sql` with `CREATE TABLE IF NOT EXISTS` for all 3 tables.

- When VIO **is** present: Frappe creates `tabCurrent State` and `tabState Transition`
  via `bench migrate`; SS's schema.sql is a no-op for those two (IF NOT EXISTS).
  SS still creates `tabDowntime Interval` (not a Frappe Doctype).
- When VIO **is not** present: SS runs schema.sql itself and creates all 3 tables.
  This is the "no VIO dependency" guarantee.

---

## Key invariants

- SS sets `modified_by = 'status-server'` and `owner = 'status-server'` on all rows.
- SS never deletes rows from any table.
- `attribute_id` is SS's internal integer, assigned sequentially from the XML config.
  It is **not** a Frappe document name — it is a stable integer for the lifetime of
  a given SS configuration.
- `attribute_name` is the full device path: e.g. `tango://host:10000/sys/dev/1/Attr`.
  Use it as the human-readable label in VIO dashboards.
- A `tabDowntime Interval` row with `closed_at = NULL` means the attribute is
  **currently down**. VIO should not process it yet.
