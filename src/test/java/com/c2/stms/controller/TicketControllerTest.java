package com.c2.stms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c2.stms.domain.InvalidStateTransitionException;
import com.c2.stms.domain.TicketNotFoundException;
import com.c2.stms.domain.TicketPriority;
import com.c2.stms.domain.TicketStatus;
import com.c2.stms.infrastructure.CommentEntity;
import com.c2.stms.infrastructure.TicketEntity;
import com.c2.stms.service.ActorContext;
import com.c2.stms.service.CreateTicketCommand;
import com.c2.stms.service.TicketDetail;
import com.c2.stms.service.TicketListQuery;
import com.c2.stms.service.TicketPatch;
import com.c2.stms.service.TicketService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TicketController.class)
@Import(GlobalExceptionHandler.class)
class TicketControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TicketService tickets;
  @MockitoBean private ActorContext actors;

  @BeforeEach
  void actor() {
    when(actors.requireActorId()).thenReturn(7L);
  }

  @Test
  void createReturns201AndLocation() throws Exception {
    TicketEntity saved = sampleTicket(12L, TicketStatus.OPEN);
    when(tickets.create(eq(7L), any(CreateTicketCommand.class))).thenReturn(saved);

    mockMvc
        .perform(
            post("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Cannot reset password","description":"Reset email never arrives.","priority":"HIGH"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/tickets/12"))
        .andExpect(jsonPath("$.id").value(12))
        .andExpect(jsonPath("$.status").value("OPEN"))
        .andExpect(jsonPath("$.comments").doesNotExist());
  }

  @Test
  void createBlankTitleReturnsStructuredValidationError() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"","description":"Body"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.path").value("/api/v1/tickets"))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void listEmptyReturns200() throws Exception {
    when(tickets.list(any(TicketListQuery.class)))
        .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

    mockMvc
        .perform(get("/api/v1/tickets"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20));
  }

  @Test
  void listUnknownStatusReturns400() throws Exception {
    mockMvc
        .perform(get("/api/v1/tickets").param("status", "BOGUS"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void getReturnsDetailWithComments() throws Exception {
    TicketEntity ticket = sampleTicket(12L, TicketStatus.OPEN);
    CommentEntity comment = new CommentEntity();
    comment.setId(1L);
    comment.setTicketId(12L);
    comment.setBody("Tried a second mailbox.");
    comment.setAuthorId(7L);
    when(tickets.get(12L)).thenReturn(new TicketDetail(ticket, List.of(comment)));

    mockMvc
        .perform(get("/api/v1/tickets/12"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(12))
        .andExpect(jsonPath("$.comments[0].body").value("Tried a second mailbox."));
  }

  @Test
  void getMissingReturns404() throws Exception {
    when(tickets.get(99L)).thenThrow(new TicketNotFoundException(99L));

    mockMvc
        .perform(get("/api/v1/tickets/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
  }

  @Test
  void getNonNumericIdReturns400() throws Exception {
    mockMvc
        .perform(get("/api/v1/tickets/abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void patchIllegalTransitionReturns409() throws Exception {
    when(tickets.patch(eq(12L), any(TicketPatch.class)))
        .thenThrow(
            new InvalidStateTransitionException(TicketStatus.CLOSED, TicketStatus.OPEN));

    mockMvc
        .perform(
            patch("/api/v1/tickets/12")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status":"OPEN"}
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ILLEGAL_TICKET_TRANSITION"))
        .andExpect(jsonPath("$.status").value(409));
  }

  @Test
  void patchBogusStatusReturns400() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/tickets/12")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status":"BOGUS"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void patchEmptyBodyReturns400() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/tickets/12")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void patchSuccessReturns200WithoutComments() throws Exception {
    when(tickets.patch(eq(12L), any(TicketPatch.class)))
        .thenReturn(sampleTicket(12L, TicketStatus.IN_PROGRESS));

    mockMvc
        .perform(
            patch("/api/v1/tickets/12")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status":"IN_PROGRESS"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.comments").doesNotExist());
  }

  private static TicketEntity sampleTicket(long id, TicketStatus status) {
    TicketEntity entity = new TicketEntity();
    entity.setId(id);
    entity.setTitle("Cannot reset password");
    entity.setDescription("Reset email never arrives.");
    entity.setStatus(status);
    entity.setPriority(TicketPriority.HIGH);
    entity.setReporterId(7L);
    Instant now = Instant.parse("2026-09-21T10:00:00Z");
    entity.setUpdatedAt(now);
    return entity;
  }
}
