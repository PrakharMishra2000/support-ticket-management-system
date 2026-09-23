# STMS implementation tasks

Bite-sized work ordered by dependency. Do not skip the state-machine rejection ITs. Follow `.cursor/rules/java-springboot.mdc`, `.cursor/rules/api-standards.mdc`, `.cursor/rules/testing.mdc`. Specs: [requirements.md](requirements.md), [data-model.md](data-model.md), [api-contract.md](api-contract.md), [state-machine.md](state-machine.md), [architecture.md](architecture.md), [test-strategy.md](test-strategy.md).

Single Maven module, packages under `com.c2.stms`. Records for DTOs. Constructor injection. No Lombok on domain. Thymeleaf for server-rendered pages. No wildcard CORS. No secrets in code.

**Out of scope this revision:** ticket/comment delete, comment edit, SSO, attachments, PUT.

Temporary actor: mutating requests read `X-Actor-Id` (positive long). Missing/invalid → **401** `UNAUTHORIZED`. Real auth replaces this header later.

Mark each task `[ ]` → `[x]` when its **Done when** is true.

---

## 1. Database & Spring Boot scaffolding

- [x] **1.1** Add `spring-boot-starter-validation`, PostgreSQL driver, and Flyway (or Liquibase). Do not add other libraries.
  - **Done when:** `pom.xml` compiles; versions pinned via Boot parent.

- [x] **1.2** Configure `application.properties` (and `application-test.properties`) with datasource placeholders (env vars, no passwords in git), JPA `ddl-auto=validate`, Flyway enabled, Jackson JavaTime UTC.
  - **Done when:** App fails fast if DB URL missing; no credentials committed.

- [x] **1.3** Flyway `V1__ticket_status_and_priority_enums.sql` matching [data-model.md](data-model.md) (`OPEN`…`CANCELLED`, `LOW`…`URGENT`).
  - **Done when:** Migration applies on empty Postgres.

- [x] **1.4** Flyway `V2__tickets.sql`: `tickets` table, checks, indexes (`status`, `priority`, `assignee_id`, `reporter_id`, `created_at DESC`, `lower(category)`).
  - **Done when:** Schema matches data-model; identity PK.

- [x] **1.5** Flyway `V3__comments.sql`: `comments` table, FK to `tickets`, body/author checks, index `(ticket_id, created_at)`.
  - **Done when:** FK and append-only columns exist; no `updated_at` on comments.

- [x] **1.6** Restrict CORS to one configured external-client origin (property `stms.cors.allowed-origin`); same-origin Thymeleaf pages do not need CORS.
  - **Done when:** Allowed origin works; `*` is not configured.

- [x] **1.7** Create empty packages: `controller`, `api`, `service`, `domain`, `infrastructure`.
  - **Done when:** Tree matches [architecture.md](architecture.md) §1.6.

---

## 2. Domain model & State Machine implementation

- [x] **2.1** Enums `TicketStatus` (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`) and `TicketPriority` in `com.c2.stms.domain`. No Spring imports.
  - **Done when:** Five statuses, four priorities; unit-testable without Spring.

- [x] **2.2** `TicketStatus.canTransition(TicketStatus target)`: allowed edges only from [state-machine.md](state-machine.md); same status → true (no-op); everything else → false.
  - **Done when:** `TicketStatusTest` covers the 5×5 matrix (A/S vs F).

- [x] **2.3** `InvalidStateTransitionException` (from, to, message). No Spring Web. (Named per implementation; maps to API `ILLEGAL_TICKET_TRANSITION` in §4.)
  - **Done when:** Constructed with both statuses.

- [x] **2.4** `TicketNotFoundException`, `UnknownAssigneeException` in domain.
  - **Done when:** Thrown types exist for handlers in §4.

- [x] **2.5** `TicketStateMachine.apply(current, target)`: no-op if equal; if `canTransition` then return `target`; else throw `InvalidStateTransitionException`.
  - **Done when:** Domain tests for allowed, same, `CLOSED`→`OPEN`, `RESOLVED`→`OPEN`, `CANCELLED`→`OPEN`, `OPEN`→`RESOLVED`/`CLOSED`, `IN_PROGRESS`→`CLOSED`. Assert outcomes, not a framework spy ([testing.mdc](.cursor/rules/testing.mdc)).

- [x] **2.6** JPA `TicketEntity` / `CommentEntity` in `infrastructure` (column names snake_case). Enums stored as PostgreSQL types or `STRING` matching DDL. No Web types.
  - **Done when:** Entities map 1:1 to tables; domain does not depend on entities.

- [x] **2.7** Spring Data `TicketRepository`, `CommentRepository` in `infrastructure`. Custom list/search query deferred to §3.
  - **Done when:** `save`/`findById` compile.

---

## 3. Service layer & Business logic

- [x] **3.1** Port `AssigneeDirectory.exists(long id)` and a stub that treats every `id > 0` as known except a documented test id (or empty deny-list). No user table.
  - **Done when:** Unknown id can be simulated for **400** `UNKNOWN_ASSIGNEE`.

- [x] **3.2** `ActorContext` (or filter) resolving `X-Actor-Id` → `long`. Invalid/missing → signal unauthenticated (exception for §4).
  - **Done when:** Services receive `actorId` without reading HTTP in domain.

- [x] **3.3** `TicketService.create`: trim; default priority `MEDIUM`; `status=OPEN`; `reporterId=actorId`; reject unknown assignee; persist; bump `updated_at` = `created_at`.
  - **Done when:** `TicketServiceTest` (Mockito repo) asserts OPEN + reporter + no save on validation failure.

- [x] **3.4** `TicketService.get`: load or `TicketNotFoundException`. Include comments oldest-first for detail mapping (service or dedicated query).
  - **Done when:** Missing id throws; comments ordered.

- [x] **3.5** `TicketService.patch`: apply present fields; `assigneeId`/`category` JSON null clears; ignore creating `status` on create path; on `status` use `TicketTransitions.apply`; never write `reporterId`/`id`/`createdAt` (controller rejects those fields); bump `updated_at` on mutation; illegal status must not `save` a new status.
  - **Done when:** Unit tests: field patch, null-clear assignee, allowed transition save, illegal transition no status mutation.

- [x] **3.6** `TicketService.list`: `page`/`size` bounds; sort `createdAt` DESC; `q` ILIKE title/description; AND filters (`status` IN, `priority` IN, `assigneeId` / `none`, `reporterId`, `category` ignore-case, `createdFrom`/`createdTo` inclusive). Unknown enum is a controller concern.
  - **Done when:** Repository query + service tests for AND + empty page for unknown well-formed assignee filter.

- [x] **3.7** `CommentService.add`: ticket must exist; comment allowed on `CLOSED`/`CANCELLED`; `authorId=actorId`; do not require ticket `updated_at` bump.
  - **Done when:** Unit test add on CLOSED and CANCELLED; missing ticket throws.

- [x] **3.8** `CommentService.list`: oldest-first; same pagination defaults as tickets; **404** if ticket missing; empty page if none.
  - **Done when:** Unit test empty vs missing ticket.

---

## 4. REST Controller & Global Exception Handling

DTOs are records in `com.c2.stms.api`. Controllers stay thin. Bean Validation on requests. JSON camelCase.

- [x] **4.1** Records: `ApiError`, `PageResponse<T>`, `TicketResponse` (no comments), `TicketDetailResponse`, `CommentResponse`, `CreateTicketRequest`, `PatchTicketRequest`, `CreateCommentRequest`.
  - **Done when:** Field names match [api-contract.md](api-contract.md).

- [x] **4.2** Bean Validation + trim: create/patch/comment lengths, category pattern, positive ids. `PatchTicketRequest`: reject `reporterId`/`id`/`createdAt` if present; require at least one writable field.
  - **Done when:** Invalid body → MethodValidation / `@Valid` failures.

- [x] **4.3** `GlobalExceptionHandler` `@ControllerAdvice`: map validation → **400** `VALIDATION_ERROR`; `UnknownAssigneeException` → **400** `UNKNOWN_ASSIGNEE`; unauthenticated → **401** `UNAUTHORIZED`; forbidden → **403** `FORBIDDEN`; `TicketNotFoundException` → **404** `TICKET_NOT_FOUND`; `InvalidStateTransitionException` → **409** `ILLEGAL_TICKET_TRANSITION`; other → **500** `INTERNAL_ERROR` (no stack in body). Body: `timestamp`, `status`, `error`, `message`, `path`, `code`.
  - **Done when:** Handler tests or `@WebMvcTest` assert JSON shape.

- [x] **4.4** `POST /api/v1/tickets` → 201 + `Location: /api/v1/tickets/{id}` + Ticket (no comments). Reject `status` on create.
  - **Done when:** `@WebMvcTest` 201/400.

- [x] **4.5** `GET /api/v1/tickets` query params per contract (`q`, repeatable `status`/`priority`, `assigneeId=none`, dates). Malformed enum/path/query → 400. Empty list → 200.
  - **Done when:** `@WebMvcTest` empty 200 + validation 400.

- [x] **4.6** `GET /api/v1/tickets/{id}` detail with comments. Non-numeric id → 400. Missing → 404.
  - **Done when:** `@WebMvcTest` 200/400/404.

- [x] **4.7** `PATCH /api/v1/tickets/{id}` → 200 Ticket without comments; 409 body `ILLEGAL_TICKET_TRANSITION` on domain exception (handler).
  - **Done when:** `@WebMvcTest` 200/400/404/409 mapping (mocked service).

- [x] **4.8** `POST /api/v1/tickets/{id}/comments` → 201 + Location; `GET .../comments` paged.
  - **Done when:** `@WebMvcTest` 201/200/404.

---

## 5. Integration tests (state machine rejection)

`*IT` with real service + DB ([test-strategy.md](test-strategy.md) §2). Assert DB after HTTP. Do not `verify` a state-machine mock.

- [x] **5.1** Test slice: `@SpringBootTest` + MockMvc + test Postgres (Testcontainers **after** driver exists, or dedicated test DB). Flyway on schema.
  - **Done when:** One smoke IT creates a ticket (`OPEN` persisted).

- [x] **5.2** Allowed ITs (one method each): `OPEN`→`IN_PROGRESS`, `OPEN`→`CANCELLED`, `IN_PROGRESS`→`RESOLVED`, `IN_PROGRESS`→`CANCELLED`, `RESOLVED`→`CLOSED`. Assert 200, body status, reload DB, `updated_at` changed.
  - **Done when:** Five green ITs.

- [x] **5.3** Rejection ITs — explicit reopens: `CLOSED`→`OPEN`, `RESOLVED`→`OPEN`, `CANCELLED`→`OPEN`. Assert **409**, `code` `ILLEGAL_TICKET_TRANSITION`, **reload status unchanged**.
  - **Done when:** Three green ITs.

- [x] **5.4** Rejection ITs — invalid jumps: `OPEN`→`RESOLVED`, `OPEN`→`CLOSED`, `IN_PROGRESS`→`CLOSED`, `IN_PROGRESS`→`OPEN`, `RESOLVED`→`CANCELLED`. Same 409 + unchanged row.
  - **Done when:** Five green ITs.

- [x] **5.5** Terminal ITs: from `CLOSED` and from `CANCELLED`, PATCH each other status → 409, row unchanged. Same-status PATCH → 200, status unchanged.
  - **Done when:** Terminal + no-op ITs green.

- [x] **5.6** `PATCH {"status":"BOGUS"}` → **400** `VALIDATION_ERROR`, row unchanged (not 409).
  - **Done when:** IT green.

- [x] **5.7** `POST` comment on `CLOSED` and `CANCELLED` → **201**; ticket status unchanged.
  - **Done when:** Two ITs green.

- [x] **5.8** `GET /api/v1/tickets?status=CANCELLED` returns only cancelled tickets.
  - **Done when:** IT green.

---

## 6. Thymeleaf Frontend UI & API integration

Server-rendered templates live under `src/main/resources/templates/` per [architecture.md](architecture.md). Browser calls use relative `/api/v1/...` URLs. Client validation mirrors §3 of requirements; the API remains authoritative.

- [x] **6.1** Add Thymeleaf, `TicketViewController`, shared `layout.html`, navigation, CSS/Tailwind links, and page shells.
  - **Done when:** `/`, `/tickets`, `/tickets/new`, and `/tickets/{id}` render through the shared layout.

- [ ] **6.2** Add same-origin JavaScript client for GET/POST/PATCH tickets and GET/POST comments; parse `ApiError`.
  - **Done when:** Calls match [api-contract.md](api-contract.md) (`/api/v1/...`).

- [ ] **6.3** Add client validation and `allowedTargets(status)` from [state-machine.md](state-machine.md).
  - **Done when:** Tests cover limits and no-reopen targets.

- [x] **6.4** Add reusable Thymeleaf fragments for status badges; distinguish `CANCELLED` from `CLOSED`.
  - **Done when:** Rendered-fragment tests cover all five statuses.

- [ ] **6.5** Implement list search/filter/table/pagination in `tickets/list.html`, including `CANCELLED` and empty state.
  - **Done when:** Controls produce API query parameters and render responses.

- [x] **6.6** Implement create form in `tickets/create.html`; no status input; mirror field limits.
  - **Done when:** Valid form POSTs and API validation errors render beside fields.

- [x] **6.7** Implement `tickets/detail.html`: editable fields and status targets only; disable status controls when terminal.
  - **Done when:** OPEN exposes IN_PROGRESS/CANCELLED, etc.

- [x] **6.8** Add comment list (oldest-first) and form enabled on CLOSED/CANCELLED.
  - **Done when:** Rendered view tests pass.

- [ ] **6.9** Use same-origin API calls and document `X-Actor-Id` as temporary development identity.
  - **Done when:** No CORS exception is needed for Thymeleaf pages.

---

## 7. End-to-end verification and error handling display

Manual/scripted against running API + UI. Record gaps vs spec; do not invent behavior.

- [ ] **7.1** Create → list → detail → comment happy path; new ticket badge `OPEN`.
  - **Done when:** Data matches API responses.

- [ ] **7.2** Walk allowed chain `OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`; cancel from OPEN and from IN_PROGRESS. UI badge updates; no reopen control.
  - **Done when:** Matches state-machine allowed edges.

- [ ] **7.3** Force illegal PATCH (devtools) `CLOSED`→`OPEN` (and one skip-ahead). UI shows **409** `message`/`code` (`ILLEGAL_TICKET_TRANSITION`); reload still old status.
  - **Done when:** Error banner/toast uses API body; no silent success.

- [ ] **7.4** Validation: empty title, overlong body, bogus status. UI shows **400** `VALIDATION_ERROR` field message.
  - **Done when:** Display uses `message`, not a generic only.

- [ ] **7.5** Unknown ticket URL → **404** `TICKET_NOT_FOUND` page/banner. Missing `X-Actor-Id` on POST → **401** display.
  - **Done when:** 404/401 surfaces are distinct from 409.

- [ ] **7.6** 500 generic message only (trigger via a test-only fault or mock); no stack trace in UI.
  - **Done when:** User sees safe text.

- [ ] **7.7** Filter/search E2E: `q`, `status=CANCELLED`, `assigneeId=none`, empty list **200**.
  - **Done when:** UI empty state, not an error.

- [ ] **7.8** Update [docs/prompt-history.md](../docs/prompt-history.md) if implementation prompts were used; keep README run instructions in sync (API + Thymeleaf templates).
  - **Done when:** A new developer can run stack from docs.

---

## Suggested order

`1 → 2.1–2.5 (domain tests) → 2.6–2.7 → 3 → 4 → 5 (especially 5.3–5.6) → 6 → 7`.

Do not ship REST before domain matrix tests. Do not ship UI before rejection ITs for reopen (`CLOSED`/`RESOLVED`/`CANCELLED` → `OPEN`).
