# Support Ticket Management System

STMS is a Java 21 and Spring Boot application for creating, searching, assigning,
updating, and commenting on support tickets. It provides both a server-rendered
Thymeleaf interface and a versioned JSON REST API.

## Features

- Create tickets with title, description, priority, and optional assignee
- Search ticket titles and descriptions
- Filter tickets by status through the dashboard
- View complete ticket details and comments
- Update ticket priority and assignee
- Add comments in every ticket state, including terminal states
- Enforce a domain-level ticket status state machine
- Return structured JSON errors from the REST API
- Render responsive Thymeleaf views styled with Tailwind CSS
- Run locally without an external database or Docker
- Inspect development data through the H2 web console

## Technology

- Java 21
- Spring Boot 4.1.1
- Spring MVC
- Spring Data JPA and Hibernate
- Thymeleaf
- Jakarta Bean Validation
- H2 in-memory database
- Tailwind CSS 3.4.17 through the Play CDN
- Maven Wrapper
- JUnit 5, Mockito, MockMvc, and Spring Boot Test

## Prerequisites

- JDK 21
- Internet access on the first build so Maven can download dependencies
- Internet access in the browser for Tailwind CDN styling

No local database, Node.js installation, or Docker runtime is required.

Verify Java before starting:

```bash
java -version
```

## Run the application

From the project root:

```bash
./mvnw spring-boot:run
```

Open:

- Application: <http://localhost:8080/tickets>
- Create ticket: <http://localhost:8080/tickets/new>
- H2 console: <http://localhost:8080/h2-console>

To build and run the packaged application:

```bash
./mvnw clean package
java -jar target/stms-0.0.1-SNAPSHOT.jar
```

## H2 database

The default datasource is:

```properties
spring.datasource.url=jdbc:h2:mem:ticketdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL
spring.datasource.username=sa
spring.datasource.password=
```

Use these H2 console values:

- JDBC URL: `jdbc:h2:mem:ticketdb`
- User Name: `sa`
- Password: leave blank

`DB_CLOSE_DELAY=-1` keeps the database alive while the application JVM is
running, even when individual connections close. This is still an in-memory
database: all data is lost when the application stops or restarts.

Hibernate manages the development schema using:

```properties
spring.jpa.hibernate.ddl-auto=update
```

The SQL files under `src/main/resources/db/migration` are retained historical
PostgreSQL migrations and are not executed because Flyway is disabled.

## Thymeleaf interface

The browser interface uses these routes:

- `GET /` redirects to `/tickets`
- `GET /tickets` displays and filters the ticket dashboard
- `GET /tickets/new` displays the creation form
- `POST /tickets` creates a ticket
- `GET /tickets/{id}` displays ticket details and comments
- `POST /tickets/{id}/details` updates priority and assignee
- `POST /tickets/{id}/status` performs a status transition
- `POST /tickets/{id}/comments` adds a comment

Server-rendered mutations use a temporary configurable development actor:

```properties
stms.views.actor-id=${STMS_VIEW_ACTOR_ID:1}
```

Override it when needed:

```bash
STMS_VIEW_ACTOR_ID=7 ./mvnw spring-boot:run
```

This setting is only a development identity bridge for HTML forms. It is not an
authentication or authorization system and must be replaced with authenticated
session identity before production use.

## Ticket state machine

Every new ticket starts in `OPEN`.

Allowed transitions:

```text
OPEN ──► IN_PROGRESS ──► RESOLVED ──► CLOSED
  │              │
  └──────────────┴──► CANCELLED
```

More explicitly:

- `OPEN` to `IN_PROGRESS`
- `OPEN` to `CANCELLED`
- `IN_PROGRESS` to `RESOLVED`
- `IN_PROGRESS` to `CANCELLED`
- `RESOLVED` to `CLOSED`

`CLOSED` and `CANCELLED` are terminal. Reopening and all unlisted jumps are
rejected with HTTP `409` and error code `ILLEGAL_TICKET_TRANSITION`. The previous
database state remains unchanged.

## REST API

The API prefix is `/api/v1`. JSON uses camelCase and timestamps use ISO-8601 UTC.

Available endpoints:

- `POST /api/v1/tickets`
- `GET /api/v1/tickets`
- `GET /api/v1/tickets/{id}`
- `PATCH /api/v1/tickets/{id}`
- `POST /api/v1/tickets/{id}/comments`
- `GET /api/v1/tickets/{id}/comments`

### Development actor header

Send `X-Actor-Id` on API mutations. The value must be a positive integer:

```bash
curl -i \
  -H 'Content-Type: application/json' \
  -H 'X-Actor-Id: 7' \
  -d '{
    "title": "Cannot access billing portal",
    "description": "The page returns an access denied message.",
    "priority": "HIGH",
    "assigneeId": 44
  }' \
  http://localhost:8080/api/v1/tickets
```

Missing required actor identity returns:

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Authenticated actor is required",
  "code": "UNAUTHORIZED"
}
```

The actor header is a temporary development mechanism and is not trusted
production authentication.

### List, search, and filter

Basic list:

```bash
curl http://localhost:8080/api/v1/tickets
```

Search and filter:

```bash
curl 'http://localhost:8080/api/v1/tickets?q=login&status=OPEN&page=0&size=20'
```

Supported query parameters include:

- `q`: case-insensitive title and description search, maximum 200 characters
- `status`: repeatable ticket status filter
- `priority`: repeatable priority filter
- `assigneeId`: positive ID or `none`
- `reporterId`: positive ID
- `category`: case-insensitive exact match
- `createdFrom` and `createdTo`: inclusive ISO-8601 instants
- `page`: zero-based page, default `0`
- `size`: page size from 1 to 100, default `20`

### Read a ticket

```bash
curl http://localhost:8080/api/v1/tickets/1
```

The detail response includes comments ordered oldest first.

### Update a ticket

Omitted fields remain unchanged. `null` clears `assigneeId` or `category`.

```bash
curl -i -X PATCH \
  -H 'Content-Type: application/json' \
  -H 'X-Actor-Id: 7' \
  -d '{
    "priority": "URGENT",
    "status": "IN_PROGRESS"
  }' \
  http://localhost:8080/api/v1/tickets/1
```

### Add a comment

```bash
curl -i \
  -H 'Content-Type: application/json' \
  -H 'X-Actor-Id: 7' \
  -d '{"body":"Investigating the access policy now."}' \
  http://localhost:8080/api/v1/tickets/1/comments
```

Comments are allowed on `CLOSED` and `CANCELLED` tickets to preserve the audit
trail.

## Validation

Important input limits:

- Title: required, 1–200 characters after trimming
- Description: required, 1–8000 characters after trimming
- Comment: required, 1–4000 characters after trimming
- Priority: `LOW`, `MEDIUM`, `HIGH`, or `URGENT`
- Category: optional, maximum 64 letters, digits, spaces, hyphens, or underscores
- Assignee ID: optional positive integer
- Search query: maximum 200 characters and no control characters

The current stub assignee directory accepts every positive ID except
`999999999`, which is reserved for exercising `UNKNOWN_ASSIGNEE` behavior.

## API errors

API failures use a consistent payload containing:

- `timestamp`
- `status`
- `error`
- `message`
- `path`
- `code`

Defined error codes:

- `VALIDATION_ERROR`
- `UNKNOWN_ASSIGNEE`
- `UNAUTHORIZED`
- `FORBIDDEN`
- `TICKET_NOT_FOUND`
- `ILLEGAL_TICKET_TRANSITION`
- `INTERNAL_ERROR`

Unexpected errors return a generic message without exposing stack traces.

## Configuration

Supported environment variables:

- `STMS_VIEW_ACTOR_ID`: temporary actor used by server-rendered forms; default `1`
- `STMS_CORS_ALLOWED_ORIGIN`: allowed origin for API CORS; default
  `http://localhost:3000`

To use another HTTP port:

```bash
SERVER_PORT=8081 ./mvnw spring-boot:run
```

## Architecture

The project uses a layered structure:

```text
controller  → HTTP and Thymeleaf adapters
api         → request, response, and form records
service     → application use cases
domain      → ticket state-machine rules and domain exceptions
infrastructure → JPA entities, repositories, specifications, and adapters
```

Request flow:

```text
Browser/API → Controller → Service → Repository → H2
                         ↘ Domain state machine
```

Business transition rules live in the domain layer, not templates,
controllers, or repositories.

Important locations:

```text
src/main/java/com/c2/stms/
src/main/resources/templates/
src/main/resources/static/
src/test/java/com/c2/stms/
spec/
docs/
```

## Testing

Run the complete test suite:

```bash
./mvnw test
```

Run only the Thymeleaf controller tests:

```bash
./mvnw -Dtest=TicketViewControllerTest test
```

Run only the state-machine integration tests:

```bash
./mvnw -Dtest=TicketServiceIntegrationTest test
```

The test suite includes:

- Domain transition matrix tests
- Ticket and comment service unit tests
- REST controller validation tests
- Thymeleaf rendering and form tests
- JPA repository search/filter integration tests
- Full-stack state-transition persistence and HTTP mapping tests
- H2 application-context and schema tests

Tests use isolated in-memory H2 databases and do not require Docker.

## Troubleshooting

### `401 Authenticated actor is required`

For JSON API mutations, include a positive `X-Actor-Id` header. Thymeleaf forms
use `STMS_VIEW_ACTOR_ID` automatically. Restart the application after changing
configuration.

### H2 console cannot see the application database

Use exactly `jdbc:h2:mem:ticketdb`, user `sa`, and a blank password. The console
must be opened from the same running application process.

### Styling is missing

The current development UI loads Tailwind from a CDN. Confirm the browser can
reach `https://cdn.tailwindcss.com`. For production, replace the Play CDN with a
compiled and locally served Tailwind stylesheet.

### Port 8080 is already in use

Start on another port:

```bash
SERVER_PORT=8081 ./mvnw spring-boot:run
```

## Current scope and limitations

- H2 data is intentionally ephemeral.
- Authentication and authorization are not implemented.
- The configured view actor and API actor header are development-only.
- Assignees come from a stub directory; there is no user table.
- Ticket deletion is not supported.
- Comment editing and deletion are not supported.
- Attachments, SLA timers, notifications, and email ingestion are out of scope.
- Tailwind currently uses the development CDN rather than a compiled production
  asset.

## Specifications

Detailed project specifications are available in:

- [`spec/requirements.md`](spec/requirements.md)
- [`spec/architecture.md`](spec/architecture.md)
- [`spec/data-model.md`](spec/data-model.md)
- [`spec/api-contract.md`](spec/api-contract.md)
- [`spec/state-machine.md`](spec/state-machine.md)
- [`spec/test-strategy.md`](spec/test-strategy.md)
- [`spec/tasks.md`](spec/tasks.md)
