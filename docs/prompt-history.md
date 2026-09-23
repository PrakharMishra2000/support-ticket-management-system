# Prompt history

Append one entry per user prompt that drives project work. Do not paste secrets.

**Entry format**

- **Date:** ISO date
- **Prompt:** short excerpt of the user request
- **Artifacts:** files created or updated

---

## 2026-09-21 — AI steering and context

**Prompt:** Set up AI steering for a Support Ticket Management System (Java 21, Spring Boot, PostgreSQL, Thymeleaf): Cursor rules for Java/Spring Boot, testing, and API standards; a documentation skill; commands to review code, review specs, and generate tests; record session prompts here.

**Artifacts:**

- `.cursor/rules/java-springboot.mdc`
- `.cursor/rules/testing.mdc`
- `.cursor/rules/api-standards.mdc`
- `.cursor/skills/documentation/SKILL.md`
- `.cursor/commands/review-code.md`
- `.cursor/commands/review-spec.md`
- `.cursor/commands/generate-tests.md`
- `docs/prompt-history.md`

---

## 2026-09-21 — Specifications (requirements and architecture)

**Prompt:** Work strictly on specifications first—no application implementation. Create `spec/` with `requirements.md` (create, list, update, search, comment, filter tickets plus backend validation) and `architecture.md` (layered Spring Boot: Controller, Service, Repository, DTOs, Handlers; Next.js frontend layout).

**Artifacts:**

- `spec/requirements.md`
- `spec/architecture.md`
- `docs/prompt-history.md`

---

## 2026-09-21 — Data model and API contract

**Prompt:** Define data layout and API endpoints from `spec/requirements.md`: `spec/data-model.md` (tickets/comments schema) and `spec/api-contract.md` (REST payloads, validation, errors). Example paths were `/api/tickets`; contract uses canonical `/api/v1` from requirements.

**Artifacts:**

- `spec/data-model.md`
- `spec/api-contract.md`
- `docs/prompt-history.md`

---

## 2026-09-21 — State machine and test strategy

**Prompt:** Finish spec artifacts: `spec/state-machine.md` (allowed OPEN→IN_PROGRESS, IN_PROGRESS→RESOLVED, RESOLVED→CLOSED, OPEN→CANCELLED, IN_PROGRESS→CANCELLED; forbid CLOSED/RESOLVED/CANCELLED→OPEN and invalid jumps) and `spec/test-strategy.md` (unit, state-machine IT, frontend component tests).

**Artifacts:**

- `spec/state-machine.md`
- `spec/test-strategy.md`
- `spec/requirements.md` (status section aligned)
- `spec/data-model.md` (`CANCELLED` enum)
- `spec/architecture.md` (pointer)
- `spec/api-contract.md` (pointer)
- `docs/prompt-history.md`

---

## 2026-09-21 — Implementation task breakdown

**Prompt:** Read `spec/` and `.cursor/rules/`; create `spec/tasks.md` with bite-sized tasks grouped by DB/Spring scaffolding, domain & state machine, services, REST & exception handling, state-machine rejection ITs, Next.js UI, E2E/error display.

**Artifacts:**

- `spec/tasks.md`
- `docs/prompt-history.md`

---

## 2026-09-21 — Task 1 Spring Boot scaffolding

**Prompt:** Implement Task 1: initialize Spring Boot (Java 21), PostgreSQL for local/prod with persistence across restarts, H2/Testcontainers for integration tests.

**Artifacts:**

- `pom.xml`
- `docker-compose.yml`
- `src/main/resources/application.properties`
- `src/main/resources/application-local.properties`
- `src/main/resources/db/migration/V1__ticket_status_and_priority_enums.sql`
- `src/main/resources/db/migration/V2__tickets.sql`
- `src/main/resources/db/migration/V3__comments.sql`
- `src/main/java/com/c2/stms/config/CorsConfig.java`
- `src/test/java/com/c2/stms/TestcontainersConfiguration.java`
- `spec/tasks.md` (section 1 checked off)
- `docs/prompt-history.md`

---

## 2026-09-22 — Task 2 domain state machine

**Prompt:** Implement Task 2: TicketStatus enum and isolated state-machine validator per spec/state-machine.md; invalid updates throw InvalidStateTransitionException.

**Artifacts:**

- `src/main/java/com/c2/stms/domain/TicketStatus.java`
- `src/main/java/com/c2/stms/domain/TicketStateMachine.java`
- `src/main/java/com/c2/stms/domain/InvalidStateTransitionException.java`
- `src/main/java/com/c2/stms/infrastructure/TicketEntity.java`
- `src/test/java/com/c2/stms/domain/TicketStatusTest.java`
- `src/test/java/com/c2/stms/domain/TicketStateMachineTest.java`
- `spec/tasks.md`
- `docs/prompt-history.md`

---

## 2026-09-22 — Task 3 TicketService and repositories

**Prompt:** Implement Task 3: Spring Data JPA repositories and TicketService; keyword search on title/description and status filters via Specifications or custom queries per api-contract.md.

**Artifacts:**

- `src/main/java/com/c2/stms/service/TicketService.java`
- `src/main/java/com/c2/stms/infrastructure/TicketSpecifications.java`
- `src/main/java/com/c2/stms/infrastructure/TicketRepository.java`
- `src/main/java/com/c2/stms/service/CommentService.java`
- `src/test/java/com/c2/stms/service/TicketServiceTest.java`
- `src/test/java/com/c2/stms/infrastructure/TicketRepositorySearchIT.java`
- `spec/tasks.md`
- `docs/prompt-history.md`

---

## 2026-09-22 — Task 4 REST controllers and exception handler

**Prompt:** Implement Task 4: TicketController, CommentController, and @ControllerAdvice GlobalExceptionHandler; Bean Validation errors must match spec/api-contract.md error JSON.

**Artifacts:**

- `src/main/java/com/c2/stms/controller/TicketController.java`
- `src/main/java/com/c2/stms/controller/CommentController.java`
- `src/main/java/com/c2/stms/controller/GlobalExceptionHandler.java`
- `src/main/java/com/c2/stms/api/CreateTicketRequest.java`
- `src/main/java/com/c2/stms/api/PatchTicketRequest.java`
- `spec/tasks.md`
- `docs/prompt-history.md`

---

## 2026-09-22 — State-machine integration tests

**Prompt:** Run generate-tests.md; write TicketServiceIntegrationTest for state-machine transitions including CLOSED→OPEN, InvalidStateTransitionException, and HTTP error mapping.

**Artifacts:**

- `src/test/java/com/c2/stms/service/TicketServiceIntegrationTest.java`
- `spec/tasks.md` (section 5)
- `docs/prompt-history.md`

---

## 2026-09-22 — Thymeleaf frontend shell

**Prompt:** Replace the previously specified Next.js frontend with Thymeleaf; add the starter, HTML page routing, and a shared layout with headers, Tailwind/CSS, and navigation.

**Artifacts:**

- `pom.xml`
- `src/main/java/com/c2/stms/controller/TicketViewController.java`
- `src/main/resources/templates/layout.html`
- `src/main/resources/templates/tickets/list.html`
- `src/main/resources/templates/tickets/new.html`
- `src/main/resources/templates/tickets/detail.html`
- `src/main/resources/static/css/app.css`
- `src/test/java/com/c2/stms/controller/TicketViewControllerTest.java`
- `spec/architecture.md`
- `spec/requirements.md`
- `spec/test-strategy.md`
- `spec/tasks.md`
- `docs/prompt-history.md`

---

## 2026-09-22 — Ticket dashboard

**Prompt:** Build the Thymeleaf ticket dashboard with a responsive ticket list, keyword search, status filter, and preserved GET filter state.

**Artifacts:**

- `src/main/java/com/c2/stms/controller/TicketViewController.java`
- `src/main/resources/templates/tickets/list.html`
- `src/test/java/com/c2/stms/controller/TicketViewControllerTest.java`
- `docs/prompt-history.md`

---

## 2026-09-22 — Ticket creation form

**Prompt:** Build a Thymeleaf ticket creation form bound to a validated `TicketCreateForm`
DTO; add POST routing with `@Valid` and `BindingResult`, and render meaningful
field-level validation errors.

**Artifacts:**

- `src/main/java/com/c2/stms/api/TicketCreateForm.java`
- `src/main/java/com/c2/stms/controller/TicketViewController.java`
- `src/main/resources/templates/tickets/create.html`
- `src/test/java/com/c2/stms/controller/TicketViewControllerTest.java`
- `spec/architecture.md`
- `spec/tasks.md`
- `docs/prompt-history.md`

---

## 2026-09-22 — Ticket detail and comments

**Prompt:** Build the Thymeleaf ticket detail view with full ticket data, priority
and assignee controls, state-machine-aware status actions, comments, and redirect
flash alerts for invalid transitions.

**Artifacts:**

- `src/main/java/com/c2/stms/api/TicketUpdateForm.java`
- `src/main/java/com/c2/stms/api/CommentCreateForm.java`
- `src/main/java/com/c2/stms/controller/TicketViewController.java`
- `src/main/resources/templates/tickets/detail.html`
- `src/test/java/com/c2/stms/controller/TicketViewControllerTest.java`
- `spec/tasks.md`
- `docs/prompt-history.md`

---

## 2026-09-22 — H2 in-memory database

**Prompt:** Replace PostgreSQL with an in-memory H2 runtime database, enable the
H2 console, and update the architecture and data-model specifications.

**Artifacts:**

- `pom.xml`
- `src/main/resources/application.properties`
- `src/main/resources/application-local.properties`
- `src/main/java/com/c2/stms/infrastructure/TicketEntity.java`
- `src/test/resources/application.properties`
- `src/test/resources/application-h2.properties`
- `src/test/java/com/c2/stms/TestcontainersConfiguration.java` (removed)
- `src/test/java/com/c2/stms/StmsApplicationTests.java`
- `src/test/java/com/c2/stms/infrastructure/TicketRepositoryIT.java`
- `src/test/java/com/c2/stms/infrastructure/TicketRepositorySearchIT.java`
- `src/test/java/com/c2/stms/service/TicketServiceIntegrationTest.java`
- `spec/architecture.md`
- `spec/data-model.md`
- `docs/prompt-history.md`

---

## 2026-09-22 — Enterprise Thymeleaf visual refresh

**Prompt:** Modernize the Thymeleaf application with Tailwind styling, responsive
ticket tables and cards, color-coded status badges, refined forms, card-based
ticket details, workflow buttons, and dismissible transition error alerts.

**Artifacts:**

- `src/main/resources/templates/layout.html`
- `src/main/resources/templates/fragments/status.html`
- `src/main/resources/templates/tickets/list.html`
- `src/main/resources/templates/tickets/create.html`
- `src/main/resources/templates/tickets/detail.html`
- `src/main/resources/static/css/app.css`
- `src/main/resources/static/js/app.js`
- `src/test/java/com/c2/stms/controller/TicketViewControllerTest.java`
- `spec/tasks.md`
- `docs/prompt-history.md`

---

## 2026-09-22 — Thymeleaf form actor fix

**Prompt:** Fix 401 `UNAUTHORIZED` responses from normal Thymeleaf form
submissions, which cannot send the development `X-Actor-Id` header.

**Artifacts:**

- `src/main/java/com/c2/stms/controller/TicketViewController.java`
- `src/main/resources/application.properties`
- `src/test/resources/application.properties`
- `src/test/java/com/c2/stms/controller/TicketViewControllerTest.java`
- `spec/architecture.md`
- `docs/prompt-history.md`
