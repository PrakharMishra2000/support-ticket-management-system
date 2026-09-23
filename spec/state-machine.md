# Ticket status state machine

Canonical status rules for STMS. Other specs defer to this file.

Create always starts at **`OPEN`**. Clients must not send `status` on `POST /api/v1/tickets`. Status changes occur only via `PATCH /api/v1/tickets/{id}` with field `status`.

The database stores the enum; it does **not** enforce transitions. The domain/service layer does. Illegal transitions leave the row unchanged and return **409** `ILLEGAL_TICKET_TRANSITION`.

---

## States

| State | Kind | Meaning |
|-------|------|---------|
| `OPEN` | initial | New ticket; work not started |
| `IN_PROGRESS` | intermediate | Work started |
| `RESOLVED` | intermediate | Work done; pending close |
| `CLOSED` | terminal | Finished; no further status changes |
| `CANCELLED` | terminal | Abandoned; no further status changes |

---

## Allowed transitions

Only these edges are legal:

| From | To |
|------|-----|
| `OPEN` | `IN_PROGRESS` |
| `OPEN` | `CANCELLED` |
| `IN_PROGRESS` | `RESOLVED` |
| `IN_PROGRESS` | `CANCELLED` |
| `RESOLVED` | `CLOSED` |

```text
OPEN ──► IN_PROGRESS ──► RESOLVED ──► CLOSED
  │              │
  └──────────────┴──► CANCELLED
```

There is **no** reopen path. `CLOSED` and `CANCELLED` have no outgoing edges.

---

## Forbidden transitions

Explicitly forbidden (non-exhaustive of every invalid pair; all non-allowed pairs are forbidden):

| From | To | Reason |
|------|-----|--------|
| `CLOSED` | `OPEN` | No reopen from closed |
| `RESOLVED` | `OPEN` | No reopen from resolved |
| `CANCELLED` | `OPEN` | No reopen from cancelled |

Any other jump that is not in the allowed table is also forbidden, including (among others):

- Skip-ahead: `OPEN` → `RESOLVED`, `OPEN` → `CLOSED`, `IN_PROGRESS` → `CLOSED`
- Backward (except none are allowed): `IN_PROGRESS` → `OPEN`, `RESOLVED` → `IN_PROGRESS`, `CLOSED` → any, `CANCELLED` → any
- Lateral: `RESOLVED` → `CANCELLED`, `CLOSED` → `CANCELLED`, `CANCELLED` → `CLOSED`

`PATCH` with `status` equal to the current status is **not** a transition: **200**, no status change.

Unknown status strings are **400** `VALIDATION_ERROR`, not 409.

---

## Full matrix

Rows = current, columns = target. `A` = allowed, `F` = forbidden, `S` = same (no-op).

|  | OPEN | IN_PROGRESS | RESOLVED | CLOSED | CANCELLED |
|--|------|-------------|----------|--------|-----------|
| OPEN | S | A | F | F | A |
| IN_PROGRESS | F | S | A | F | A |
| RESOLVED | F | F | S | A | F |
| CLOSED | F | F | F | S | F |
| CANCELLED | F | F | F | F | S |

---

## API behavior

| Outcome | HTTP | `code` | Persistence |
|---------|------|--------|-------------|
| Allowed transition | 200 | | New `status`; `updated_at` bumped |
| Forbidden transition | 409 | `ILLEGAL_TICKET_TRANSITION` | Unchanged |
| Same status | 200 | | Unchanged status; other PATCH fields may still apply |
| Unknown enum | 400 | `VALIDATION_ERROR` | Unchanged |

Comments remain allowed on every status, including `CLOSED` and `CANCELLED`.
