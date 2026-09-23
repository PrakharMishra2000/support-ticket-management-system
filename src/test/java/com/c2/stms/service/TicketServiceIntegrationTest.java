package com.c2.stms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c2.stms.config.ActorIdFilter;
import com.c2.stms.domain.InvalidStateTransitionException;
import com.c2.stms.domain.TicketPriority;
import com.c2.stms.domain.TicketStateMachine;
import com.c2.stms.domain.TicketStatus;
import com.c2.stms.infrastructure.TicketEntity;
import com.c2.stms.infrastructure.TicketRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * State-machine integration tests (generate-tests.md + spec/test-strategy.md §2). Asserts
 * outcomes and DB state, not a mocked state machine. Illegal transitions map to HTTP 409
 * {@code ILLEGAL_TICKET_TRANSITION} per spec/api-contract.md (not 400 — that is validation —
 * and not 422).
 */
@SpringBootTest
@AutoConfigureMockMvc
class TicketServiceIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private TicketService ticketService;
  @Autowired private TicketStateMachine stateMachine;
  @Autowired private TicketRepository tickets;

  @Test
  void usesRealDomainStateMachineBean() {
    assertThat(stateMachine).isExactlyInstanceOf(TicketStateMachine.class);
  }

  @Test
  void createPersistsOpenTicket() throws Exception {
    long id = createViaApi("Smoke create");

    assertThat(tickets.findById(id).orElseThrow().getStatus()).isEqualTo(TicketStatus.OPEN);
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource({
    "OPEN, IN_PROGRESS",
    "OPEN, CANCELLED",
    "IN_PROGRESS, RESOLVED",
    "IN_PROGRESS, CANCELLED",
    "RESOLVED, CLOSED"
  })
  void allowedTransitionPersistsAndBumpsUpdatedAt(TicketStatus from, TicketStatus to)
      throws Exception {
    long id = persistWithStatus(from);
    Instant before = tickets.findById(id).orElseThrow().getUpdatedAt();
    Thread.sleep(20);

    patchStatus(id, to).andExpect(status().isOk()).andExpect(jsonPath("$.status").value(to.name()));

    TicketEntity reloaded = tickets.findById(id).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(to);
    assertThat(reloaded.getUpdatedAt()).isAfter(before);
  }

  @Test
  void serviceRejectsClosedToOpenWithInvalidStateTransitionException() {
    long id = persistWithStatus(TicketStatus.CLOSED);

    assertThatThrownBy(() -> ticketService.patch(id, statusPatch(TicketStatus.OPEN)))
        .isInstanceOf(InvalidStateTransitionException.class)
        .satisfies(
            ex -> {
              InvalidStateTransitionException invalid = (InvalidStateTransitionException) ex;
              assertThat(invalid.from()).isEqualTo(TicketStatus.CLOSED);
              assertThat(invalid.to()).isEqualTo(TicketStatus.OPEN);
            });

    assertThat(tickets.findById(id).orElseThrow().getStatus()).isEqualTo(TicketStatus.CLOSED);
  }

  @Test
  void apiRejectsClosedToOpenWithConflictNotValidation() throws Exception {
    long id = persistWithStatus(TicketStatus.CLOSED);

    patchStatus(id, TicketStatus.OPEN)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.code").value("ILLEGAL_TICKET_TRANSITION"))
        .andExpect(jsonPath("$.error").value("Conflict"))
        .andExpect(jsonPath("$.path").value("/api/v1/tickets/" + id))
        .andExpect(jsonPath("$.message").exists());

    assertThat(tickets.findById(id).orElseThrow().getStatus()).isEqualTo(TicketStatus.CLOSED);
  }

  @Test
  void resolvedToOpenIsRejectedAndLeavesRowUnchanged() throws Exception {
    assertRejectedByServiceAndApi(TicketStatus.RESOLVED, TicketStatus.OPEN);
  }

  @Test
  void cancelledToOpenIsRejectedAndLeavesRowUnchanged() throws Exception {
    assertRejectedByServiceAndApi(TicketStatus.CANCELLED, TicketStatus.OPEN);
  }

  @ParameterizedTest(name = "jump {0} -> {1}")
  @CsvSource({
    "OPEN, RESOLVED",
    "OPEN, CLOSED",
    "IN_PROGRESS, CLOSED",
    "IN_PROGRESS, OPEN",
    "RESOLVED, CANCELLED"
  })
  void invalidJumpIsRejectedAndLeavesRowUnchanged(TicketStatus from, TicketStatus to)
      throws Exception {
    long id = persistWithStatus(from);

    assertThatThrownBy(() -> ticketService.patch(id, statusPatch(to)))
        .isInstanceOf(InvalidStateTransitionException.class);

    patchStatus(id, to)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ILLEGAL_TICKET_TRANSITION"));

    assertThat(tickets.findById(id).orElseThrow().getStatus()).isEqualTo(from);
  }

  @ParameterizedTest
  @CsvSource({
    "CLOSED, IN_PROGRESS",
    "CLOSED, RESOLVED",
    "CLOSED, CANCELLED",
    "CANCELLED, IN_PROGRESS",
    "CANCELLED, RESOLVED",
    "CANCELLED, CLOSED"
  })
  void terminalStatesRejectFurtherTransitions(TicketStatus from, TicketStatus to) throws Exception {
    long id = persistWithStatus(from);

    patchStatus(id, to)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ILLEGAL_TICKET_TRANSITION"));

    assertThat(tickets.findById(id).orElseThrow().getStatus()).isEqualTo(from);
  }

  @Test
  void sameStatusPatchIsNoOpSuccess() throws Exception {
    long id = persistWithStatus(TicketStatus.CLOSED);

    patchStatus(id, TicketStatus.CLOSED)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"));

    assertThat(tickets.findById(id).orElseThrow().getStatus()).isEqualTo(TicketStatus.CLOSED);
  }

  @Test
  void bogusStatusIsValidation400NotConflict() throws Exception {
    long id = persistWithStatus(TicketStatus.OPEN);

    mockMvc
        .perform(
            patch("/api/v1/tickets/{id}", id)
                .header(ActorIdFilter.HEADER, "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status":"BOGUS"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.status").value(400));

    assertThat(tickets.findById(id).orElseThrow().getStatus()).isEqualTo(TicketStatus.OPEN);
  }

  @ParameterizedTest
  @CsvSource({"CLOSED", "CANCELLED"})
  void commentAllowedOnTerminalStatus(TicketStatus status) throws Exception {
    long id = persistWithStatus(status);

    mockMvc
        .perform(
            post("/api/v1/tickets/{id}/comments", id)
                .header(ActorIdFilter.HEADER, "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"body":"Audit note"}
                    """))
        .andExpect(status().isCreated());

    assertThat(tickets.findById(id).orElseThrow().getStatus()).isEqualTo(status);
  }

  @Test
  void listCanFilterCancelled() throws Exception {
    long cancelledId = persistWithStatus(TicketStatus.CANCELLED);
    persistWithStatus(TicketStatus.OPEN);

    mockMvc
        .perform(get("/api/v1/tickets").param("status", "CANCELLED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.id == " + cancelledId + ")]").isNotEmpty())
        .andExpect(jsonPath("$.content[*].status", everyItem(is("CANCELLED"))));
  }

  @Test
  void missingTicketIs404() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/tickets/{id}", 9_999_999L)
                .header(ActorIdFilter.HEADER, "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status":"IN_PROGRESS"}
                    """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
  }

  private ResultActions patchStatus(long id, TicketStatus to) throws Exception {
    return mockMvc.perform(
        patch("/api/v1/tickets/{id}", id)
            .header(ActorIdFilter.HEADER, "7")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"" + to.name() + "\"}"));
  }

  private void assertRejectedByServiceAndApi(TicketStatus from, TicketStatus to) throws Exception {
    long id = persistWithStatus(from);

    assertThatThrownBy(() -> ticketService.patch(id, statusPatch(to)))
        .isInstanceOf(InvalidStateTransitionException.class)
        .satisfies(
            ex -> {
              InvalidStateTransitionException invalid = (InvalidStateTransitionException) ex;
              assertThat(invalid.from()).isEqualTo(from);
              assertThat(invalid.to()).isEqualTo(to);
            });
    assertThat(tickets.findById(id).orElseThrow().getStatus()).isEqualTo(from);

    patchStatus(id, to)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.code").value("ILLEGAL_TICKET_TRANSITION"));
    assertThat(tickets.findById(id).orElseThrow().getStatus()).isEqualTo(from);
  }

  private long createViaApi(String title) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/v1/tickets")
                    .header(ActorIdFilter.HEADER, "7")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"title\":\""
                            + title
                            + "\",\"description\":\"Integration test ticket\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    int idStart = body.indexOf("\"id\":") + 5;
    int idEnd = body.indexOf(",", idStart);
    return Long.parseLong(body.substring(idStart, idEnd).trim());
  }

  private long persistWithStatus(TicketStatus status) {
    TicketEntity entity = new TicketEntity();
    entity.setTitle("IT " + status + " " + UUID.randomUUID());
    entity.setDescription("State machine integration fixture");
    entity.setStatus(status);
    entity.setPriority(TicketPriority.MEDIUM);
    entity.setReporterId(7L);
    return tickets.saveAndFlush(entity).getId();
  }

  private static TicketPatch statusPatch(TicketStatus status) {
    return new TicketPatch(null, null, null, null, false, null, false, status);
  }
}
