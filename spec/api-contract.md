# STMS API contract

REST contract for STMS. Canonical prefix is **`/api/v1`** ([requirements.md](requirements.md)). Paths such as `/api/tickets` are **not** served; clients must use `/api/v1/tickets`.

JSON: camelCase, UTF-8, `Content-Type: application/json`. Timestamps: ISO-8601 UTC.

AuthN/AuthZ product is out of scope. Mutating requests require an authenticated `actorId`. Missing auth → **401**. Authenticated but not permitted → **403**.

Validation: trim strings at the edge; empty after trim is omitted (optional) or invalid (required). Constraints match [requirements.md §3](requirements.md) and [data-model.md](data-model.md).

---

## Shared schemas

### Ticket (list item and PATCH/POST response)

Comments are **omitted** on list and on create/patch responses.

```json
{
  "id": 12,
  "title": "Cannot reset password",
  "description": "Reset email never arrives.",
  "status": "OPEN",
  "priority": "HIGH",
  "category": "account",
  "assigneeId": 44,
  "reporterId": 7,
  "createdAt": "2026-09-21T10:00:00Z",
  "updatedAt": "2026-09-21T10:00:00Z"
}
```

`assigneeId` and `category` may be `null`.

### Ticket detail (GET by id)

Same fields plus `comments` (oldest `createdAt` first).

```json
{
  "id": 12,
  "title": "Cannot reset password",
  "description": "Reset email never arrives.",
  "status": "OPEN",
  "priority": "HIGH",
  "category": "account",
  "assigneeId": 44,
  "reporterId": 7,
  "createdAt": "2026-09-21T10:00:00Z",
  "updatedAt": "2026-09-21T10:15:00Z",
  "comments": [
    {
      "id": 1,
      "ticketId": 12,
      "body": "Tried a second mailbox.",
      "authorId": 7,
      "createdAt": "2026-09-21T10:15:00Z"
    }
  ]
}
```

### Comment

```json
{
  "id": 1,
  "ticketId": 12,
  "body": "Tried a second mailbox.",
  "authorId": 7,
  "createdAt": "2026-09-21T10:15:00Z"
}
```

### Page wrapper

Used by `GET /api/v1/tickets` and `GET /api/v1/tickets/{id}/comments`.

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

### Error payload

All error responses use this body ([api-standards](../.cursor/rules/api-standards.mdc)):

```json
{
  "timestamp": "2026-09-21T10:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Cannot change status of a CLOSED ticket",
  "path": "/api/v1/tickets/12",
  "code": "ILLEGAL_TICKET_TRANSITION"
}
```

| HTTP | `code` | When |
|------|--------|------|
| 400 | `VALIDATION_ERROR` | Constraint or malformed JSON/query/path |
| 400 | `UNKNOWN_ASSIGNEE` | Create/patch `assigneeId` not a known actor |
| 401 | `UNAUTHORIZED` | No authenticated actor |
| 403 | `FORBIDDEN` | Authenticated but not allowed |
| 404 | `TICKET_NOT_FOUND` | Ticket id does not exist |
| 409 | `ILLEGAL_TICKET_TRANSITION` | Status change not allowed; ticket unchanged |
| 500 | `INTERNAL_ERROR` | Unexpected; `message` must not leak internals |

`message` for `VALIDATION_ERROR` lists failing fields (e.g. `title: must be 1-200 characters`).

`PUT` and `DELETE` on tickets/comments are **not** in this revision.

Path `{id}` and `{commentId}`: positive integers. Non-numeric path → **400** `VALIDATION_ERROR`.

---

## `POST /api/v1/tickets`

Create ticket. `status` is always `OPEN`. `reporterId` is `actorId`. Clients must not send `status`, `reporterId`, `id`, or timestamps.

**Request**

```json
{
  "title": "Cannot reset password",
  "description": "Reset email never arrives.",
  "priority": "HIGH",
  "category": "account",
  "assigneeId": 44
}
```

| Field | Required | Constraints |
|-------|----------|-------------|
| `title` | yes | 1–200 |
| `description` | yes | 1–8000 |
| `priority` | no | `LOW` \| `MEDIUM` \| `HIGH` \| `URGENT`; default `MEDIUM` |
| `category` | no | max 64; `^[A-Za-z0-9 _-]+$` |
| `assigneeId` | no | positive integer; unknown → **400** `UNKNOWN_ASSIGNEE` |

**Response:** **201** Ticket (no `comments`). Header `Location: /api/v1/tickets/{id}`.

---

## `GET /api/v1/tickets`

List, search, and filter. Sort: `createdAt` descending. Empty `content` is still **200**.

**Query**

| Param | Default | Constraints / meaning |
|-------|---------|------------------------|
| `page` | `0` | integer ≥ 0 |
| `size` | `20` | integer 1–100 |
| `q` | omitted | max 200; no control chars; case-insensitive substring on `title` and `description`; empty/omitted = no text filter |
| `status` | | repeatable; IN; values = status enum |
| `priority` | | repeatable; IN; values = priority enum |
| `assigneeId` | | positive integer, or `none` (unassigned) |
| `reporterId` | | positive integer |
| `category` | | case-insensitive exact |
| `createdFrom` | | ISO-8601 instant, inclusive |
| `createdTo` | | ISO-8601 instant, inclusive; if both set, `createdFrom` ≤ `createdTo` |

Filters combine with **AND**. Unknown enum → **400** `VALIDATION_ERROR`. Well-formed id that matches no rows → **200** empty page.

**Response:** **200** page of Ticket (no `comments`).

---

## `GET /api/v1/tickets/{id}`

**Response:** **200** Ticket detail (includes `comments`). Unknown id → **404** `TICKET_NOT_FOUND`.

---

## `PATCH /api/v1/tickets/{id}`

Partial update. Omitted fields unchanged. JSON `null` on `assigneeId` or `category` clears that field. `reporterId`, `id`, `createdAt` are not writable if sent → **400** `VALIDATION_ERROR`.

**Request** (all fields optional; at least one writable field required)

```json
{
  "title": "Password reset emails",
  "description": "Updated repro steps.",
  "priority": "URGENT",
  "category": "account",
  "assigneeId": null,
  "status": "IN_PROGRESS"
}
```

| Field | Constraints |
|-------|-------------|
| `title` | 1–200 if present |
| `description` | 1–8000 if present |
| `priority` | enum if present |
| `category` | same as create, or `null` |
| `assigneeId` | positive integer, `null` to clear; unknown integer → **400** `UNKNOWN_ASSIGNEE` |
| `status` | known status **and** allowed transition from current ([state-machine.md](state-machine.md)); else **409**, ticket unchanged |

**Response:** **200** Ticket (no `comments`). **404** if missing. Empty body / no fields → **400** `VALIDATION_ERROR`.

---

## `POST /api/v1/tickets/{id}/comments`

Allowed for every ticket status, including `CLOSED` and `CANCELLED`. `authorId` is `actorId`.

**Request**

```json
{
  "body": "Tried a second mailbox."
}
```

| Field | Required | Constraints |
|-------|----------|-------------|
| `body` | yes | 1–4000 after trim |

**Response:** **201** Comment. Header `Location: /api/v1/tickets/{id}/comments/{commentId}`. Unknown ticket → **404** `TICKET_NOT_FOUND`.

---

## `GET /api/v1/tickets/{id}/comments`

Oldest `createdAt` first. Pagination: `page`, `size` (same defaults/limits as ticket list).

**Response:** **200** page of Comment. Unknown ticket → **404** `TICKET_NOT_FOUND`. Ticket with no comments → **200** empty `content`.

---

## Endpoint summary

| Method | Path | Success |
|--------|------|---------|
| `POST` | `/api/v1/tickets` | 201 Ticket |
| `GET` | `/api/v1/tickets` | 200 page of Ticket |
| `GET` | `/api/v1/tickets/{id}` | 200 Ticket detail |
| `PATCH` | `/api/v1/tickets/{id}` | 200 Ticket |
| `POST` | `/api/v1/tickets/{id}/comments` | 201 Comment |
| `GET` | `/api/v1/tickets/{id}/comments` | 200 page of Comment |
