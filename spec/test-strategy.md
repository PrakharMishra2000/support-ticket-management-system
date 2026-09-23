# STMS test strategy

How to test STMS against [requirements.md](requirements.md), [api-contract.md](api-contract.md), and [state-machine.md](state-machine.md). Follow `.cursor/rules/testing.mdc`: JUnit 5, Mockito, narrowest Spring slice, `*Test` vs `*IT`, assert outcomes not internals. No real secrets.

Do not add test libraries unless required. Backend and Thymeleaf views use JUnit 5, Mockito, Spring Boot Test, MockMvc, and HTML assertions where useful.

---

## 1. Unit tests (`*Test`)

Pure domain and mocked-service tests. No Spring context unless a slice is the unit under test.

### 1.1 Domain status machine

Class under test: status transition policy (e.g. `TicketStatus` / `TicketStateMachine`).

- Every **allowed** edge in [state-machine.md](state-machine.md) returns success.
- Every **forbidden** cell in the 5×5 matrix (except same-status no-op) throws `IllegalTicketTransitionException` (or equivalent).
- Explicit reopen cases: `CLOSED` → `OPEN`, `RESOLVED` → `OPEN`, `CANCELLED` → `OPEN`.
- Explicit invalid jumps: `OPEN` → `RESOLVED`, `OPEN` → `CLOSED`, `IN_PROGRESS` → `CLOSED`, `IN_PROGRESS` → `OPEN`, `RESOLVED` → `CANCELLED`.
- Terminal states `CLOSED` and `CANCELLED` reject all other targets.
- Same-status is allowed as a no-op (no exception).

Do **not** `verify` a framework state-machine spy. Assert `from.canTransition(to)` / resulting status.

### 1.2 Ticket / comment services

Mock repositories. Cover:

| Case | Expectation |
|------|-------------|
| Create | `status=OPEN`, `reporterId=actorId`, default `priority=MEDIUM` |
| Create validation | blank/too-long `title`/`description` fail before save |
| Patch fields | title, description, priority, category, `assigneeId` null-clear |
| Patch forbidden fields | `reporterId` / `id` / timestamps rejected |
| Patch allowed status | repository save with new status |
| Patch illegal status | exception; **no** save of new status (verify save not called with changed status, or reload mock returns original) |
| Unknown ticket | not-found exception |
| Unknown assignee | validation/unknown-assignee exception |
| Add comment | append-only; allowed when ticket is `CLOSED` or `CANCELLED` |
| List/search/filter | `q` AND filters; pagination bounds |

### 1.3 Mapping / validation

Request record constraints (lengths, enums, category pattern, `q` control characters, `createdFrom` ≤ `createdTo`).

---

## 2. State-machine integration tests (`*IT`)

Persistence and HTTP must honor the matrix. Prefer `@DataJpaTest` + service, or `@SpringBootTest` with a test database. `@WebMvcTest` for HTTP mapping of 409/400/200 with a mocked service is a slice test, not a full state-machine IT.

### 2.1 Persistence scenarios

For each allowed edge: insert a ticket in `from`, PATCH `status=to`, reload from DB, assert `to` and `updated_at` changed.

For each explicit forbidden case (reopens + skip-ahead listed above): PATCH, assert **409** (or domain exception at service IT), reload, assert status **unchanged**.

Terminal: ticket in `CLOSED` or `CANCELLED`; any other `status` in PATCH → unchanged row.

Create: persisted status is `OPEN` even if the client omitted status (client cannot set it).

### 2.2 API scenarios (`MockMvc` / `@SpringBootTest`)

| Request | Current DB status | Result |
|---------|-------------------|--------|
| `PATCH ... {"status":"IN_PROGRESS"}` | `OPEN` | 200, body status `IN_PROGRESS` |
| `PATCH ... {"status":"CANCELLED"}` | `OPEN` | 200 `CANCELLED` |
| `PATCH ... {"status":"RESOLVED"}` | `IN_PROGRESS` | 200 |
| `PATCH ... {"status":"CANCELLED"}` | `IN_PROGRESS` | 200 |
| `PATCH ... {"status":"CLOSED"}` | `RESOLVED` | 200 |
| `PATCH ... {"status":"RESOLVED"}` | `OPEN` | 409, body `ILLEGAL_TICKET_TRANSITION`, DB still `OPEN` |
| `PATCH ... {"status":"OPEN"}` | `CLOSED` | 409, still `CLOSED` |
| `PATCH ... {"status":"OPEN"}` | `RESOLVED` | 409, still `RESOLVED` |
| `PATCH ... {"status":"OPEN"}` | `CANCELLED` | 409, still `CANCELLED` |
| `PATCH ... {"status":"BOGUS"}` | any | 400 `VALIDATION_ERROR` |
| `POST /comments` | `CLOSED` or `CANCELLED` | 201 |

List filter `status=CANCELLED` returns only cancelled tickets.

---

## 3. Thymeleaf view tests

Use `@WebMvcTest(TicketViewController.class)` for route/view/model assertions and rendered HTML checks.

| View / fragment | Cases |
|-----------------|-------|
| `layout.html` | Shared title/head, navigation links, local CSS and pinned Tailwind stylesheet |
| Ticket list | `/tickets` view, search/filter controls, empty state, pagination |
| Create ticket | `/tickets/new`; required title/description; no create-status control |
| Ticket detail | `/tickets/{id}` model id; allowed status targets only; terminal controls disabled |
| Comments | Oldest-first output; form enabled on `CLOSED` and `CANCELLED` |
| API error display | 400 field error, 404, and 409 transition message/code are visible and distinct |

Do not assert on private CSS class names. Assert routes, view names, model attributes, roles, labels, and visible text. Render user input with `th:text`, never `th:utext`.

---

## 4. Layering of tests

```text
Domain matrix          *Test     (no Spring)
Service use-cases      *Test     (Mockito repositories)
Web validation/HTTP    *Test     (@WebMvcTest)
State + DB             *IT       (@DataJpaTest or Spring Boot + DB)
API state + HTTP       *IT       (MockMvc + real service/DB)
Thymeleaf views        *Test     (@WebMvcTest + rendered HTML)
```

Persistence ITs may use an in-memory or Testcontainers PostgreSQL **after** persistence is in the project. Until then, domain and `@WebMvcTest` coverage of the matrix is required; DB ITs are deferred, not skipped from the plan.
