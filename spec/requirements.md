# STMS functional requirements

This document is the product spec for the Support Ticket Management System (STMS). It defines behavior only. Application code is out of scope until this spec is accepted.

Base path: `/api/v1`. Error bodies and status codes follow `.cursor/rules/api-standards.mdc`.

Authentication mechanism is **out of scope** for this revision. Requirements assume an authenticated actor identity (`actorId`) is present on mutating requests. Unauthenticated calls return **401**. Actors without rights on a ticket return **403**.

---

## 1. Domain model

### 1.1 Ticket

| Field | Type | Notes |
|-------|------|--------|
| `id` | integer (generated) | Stable identifier |
| `title` | string | Short summary |
| `description` | string | Problem details |
| `status` | enum | See state machine |
| `priority` | enum | `LOW`, `MEDIUM`, `HIGH`, `URGENT` |
| `category` | string | Free-text label, optional |
| `assigneeId` | integer or null | Optional owner |
| `reporterId` | integer | Set from `actorId` on create; immutable |
| `createdAt` | instant (UTC) | System-set |
| `updatedAt` | instant (UTC) | System-set on every mutation |

### 1.2 Comment

| Field | Type | Notes |
|-------|------|--------|
| `id` | integer (generated) | |
| `ticketId` | integer | Parent ticket |
| `body` | string | Comment text |
| `authorId` | integer | Set from `actorId`; immutable |
| `createdAt` | instant (UTC) | System-set; comments are append-only |

### 1.3 Ticket status machine

Canonical rules: [state-machine.md](state-machine.md).

Statuses: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`.

Allowed: `OPEN` → `IN_PROGRESS`, `OPEN` → `CANCELLED`, `IN_PROGRESS` → `RESOLVED`, `IN_PROGRESS` → `CANCELLED`, `RESOLVED` → `CLOSED`.

Forbidden: `CLOSED` → `OPEN`, `RESOLVED` → `OPEN`, `CANCELLED` → `OPEN`, and any other jump not listed as allowed. `CLOSED` and `CANCELLED` are terminal. No reopen.

Illegal transitions leave `status` unchanged and return **409** `ILLEGAL_TICKET_TRANSITION`.

Create always starts at `OPEN`. Clients must not send `status` on create.

---

## 2. Functional requirements

### 2.1 Create ticket

`POST /api/v1/tickets`

- Accepts `title`, `description`, optional `priority` (default `MEDIUM`), optional `category`, optional `assigneeId`.
- Persists ticket with `status=OPEN`, `reporterId=actorId`.
- Returns **201**, body = ticket resource, `Location: /api/v1/tickets/{id}`.
- Validation failures: **400**. Unknown `assigneeId`: **400** with `code` `UNKNOWN_ASSIGNEE`.

### 2.2 List tickets

`GET /api/v1/tickets`

- Returns a page of tickets, newest `createdAt` first.
- Query parameters: pagination (`page` default `0`, `size` default `20`, max `100`) plus the filters in §2.6.
- Response: `{ "content": [ Ticket ], "page": 0, "size": 20, "totalElements": n, "totalPages": n }`.
- **200** even when `content` is empty.

### 2.3 Get ticket

`GET /api/v1/tickets/{id}`

- Returns the ticket and its comments (oldest `createdAt` first).
- Unknown id: **404** with `code` `TICKET_NOT_FOUND`.

### 2.4 Update ticket

`PATCH /api/v1/tickets/{id}`

- Partial update of `title`, `description`, `priority`, `category`, `assigneeId` (null clears assignee).
- `status` is updated only through this PATCH when the pair `(current, new)` is an allowed transition. Prefer a dedicated field `status` on the same PATCH; do not use a separate undocumented path.
- `reporterId`, `id`, `createdAt` are not writable.
- Success: **200** and the updated ticket.
- Unknown id: **404**. Illegal status: **409**. Validation: **400**. Concurrent lost update is not required in this revision.

`PUT /api/v1/tickets/{id}` is **not** required in this revision (avoid full-replace surprises on optional fields).

Delete of tickets is **not** required in this revision.

### 2.5 Search tickets

`GET /api/v1/tickets?q={query}` (`q` combined with list)

- `q` is a case-insensitive substring match on `title` and `description`.
- Does not search comment bodies in this revision.
- Empty or omitted `q` means no text constraint.
- Combined with §2.6 filters using **AND**.

### 2.6 Filter tickets

Same list endpoint. All filters optional; combine with **AND**.

| Param | Meaning |
|-------|---------|
| `status` | Exact match; repeatable (`status=OPEN&status=IN_PROGRESS`) means IN |
| `priority` | Exact match; repeatable means IN |
| `assigneeId` | Exact match; `assigneeId=none` means unassigned |
| `reporterId` | Exact match |
| `category` | Case-insensitive exact match |
| `createdFrom` / `createdTo` | Inclusive UTC instants on `createdAt` |

Unknown enum values: **400** with `code` `VALIDATION_ERROR`.

### 2.7 Comment on ticket

`POST /api/v1/tickets/{id}/comments`

- Body: `{ "body": "..." }`.
- Allowed on every status including `CLOSED` and `CANCELLED` (audit trail).
- Returns **201**, comment resource, `Location: /api/v1/tickets/{id}/comments/{commentId}`.
- Unknown ticket: **404**. Validation: **400**.

`GET /api/v1/tickets/{id}/comments` returns comments oldest-first (same pagination defaults as list). **200**.

Comments cannot be edited or deleted in this revision.

---

## 3. Backend input validation

Validate at the HTTP edge before persistence. Fail closed. Field errors use **400**, `code` `VALIDATION_ERROR`, and `message` listing fields. Trim leading/trailing whitespace on strings before length checks. Empty string after trim is treated as omitted for optional fields and as invalid for required fields.

### 3.1 Ticket create / patch

| Field | Create | Patch | Rules |
|-------|--------|-------|--------|
| `title` | required | optional | 1–200 characters |
| `description` | required | optional | 1–8000 characters |
| `priority` | optional | optional | One of `LOW`, `MEDIUM`, `HIGH`, `URGENT` |
| `category` | optional | optional | Max 64 characters; letters, digits, space, hyphen, underscore |
| `assigneeId` | optional | optional | Positive integer, or JSON `null` on patch to clear |
| `status` | forbidden | optional | Must be a known status **and** an allowed transition from current |
| `reporterId` | forbidden | forbidden | |
| `id` / timestamps | forbidden | forbidden | |

Path `{id}`: positive integer. Malformed path: **400**.

### 3.2 Comments

| Field | Rules |
|-------|--------|
| `body` | Required; 1–4000 characters after trim |

### 3.3 Query parameters

| Param | Rules |
|-------|--------|
| `page` | Integer ≥ 0 |
| `size` | Integer 1–100 |
| `q` | Max 200 characters; no control characters |
| `createdFrom` / `createdTo` | ISO-8601 instants; if both set, `createdFrom` ≤ `createdTo` |
| `assigneeId` / `reporterId` | Positive integer, except `assigneeId=none` |

### 3.4 Persistence / integrity

- Unknown ticket id on get/update/comment: **404** `TICKET_NOT_FOUND`.
- Unknown `assigneeId` / `reporterId` filter that is not a well-formed integer: **400**.
- Filter by a well-formed id that does not exist: **200** with empty page (not 404).

---

## 4. Out of scope (this revision)

- Attachments, tags beyond `category`, SLA timers, email ingest
- Editing or deleting comments; deleting tickets
- Full-text search engines; searching comment bodies
- AuthN/AuthZ product (SSO, roles) beyond 401/403 placeholders
- A separate Next.js frontend (UI is server-rendered Thymeleaf per `spec/architecture.md`)
