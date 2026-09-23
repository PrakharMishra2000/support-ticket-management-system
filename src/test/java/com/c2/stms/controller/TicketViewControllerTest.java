package com.c2.stms.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.c2.stms.domain.TicketPriority;
import com.c2.stms.domain.TicketStatus;
import com.c2.stms.domain.InvalidStateTransitionException;
import com.c2.stms.infrastructure.TicketEntity;
import com.c2.stms.service.ActorContext;
import com.c2.stms.service.CommentService;
import com.c2.stms.service.CreateTicketCommand;
import com.c2.stms.service.TicketDetail;
import com.c2.stms.service.TicketListQuery;
import com.c2.stms.service.TicketPatch;
import com.c2.stms.service.TicketService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = TicketViewController.class, properties = "stms.views.actor-id=7")
class TicketViewControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ActorContext actorContext;
  @MockitoBean private CommentService commentService;
  @MockitoBean private TicketService ticketService;

  @BeforeEach
  void emptyTicketPage() {
    when(ticketService.list(any(TicketListQuery.class)))
        .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
  }

  @Test
  void homeRedirectsToTicketList() throws Exception {
    mockMvc
        .perform(get("/"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/tickets"));
  }

  @Test
  void ticketListRendersSharedLayout() throws Exception {
    mockMvc
        .perform(get("/tickets"))
        .andExpect(status().isOk())
        .andExpect(view().name("tickets/list"))
        .andExpect(content().string(containsString("Primary navigation")))
        .andExpect(content().string(containsString("/css/app.css")));
  }

  @Test
  void dashboardRendersTicketsAndPreservesSearchAndStatus() throws Exception {
    TicketEntity assigned = ticket(12L, "Password reset", TicketStatus.IN_PROGRESS, 44L);
    TicketEntity unassigned = ticket(13L, "VPN issue", TicketStatus.OPEN, null);
    when(ticketService.list(any(TicketListQuery.class)))
        .thenReturn(new PageImpl<>(List.of(assigned, unassigned), PageRequest.of(0, 20), 2));

    mockMvc
        .perform(get("/tickets").param("q", " password ").param("status", "IN_PROGRESS"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("query", "password"))
        .andExpect(model().attribute("selectedStatus", TicketStatus.IN_PROGRESS))
        .andExpect(content().string(containsString("value=\"password\"")))
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        "(?s).*value=\"IN_PROGRESS\"\\s+selected=\"selected\".*")))
        .andExpect(content().string(containsString("Password reset")))
        .andExpect(content().string(containsString("User 44")))
        .andExpect(content().string(containsString("Unassigned")));

    ArgumentCaptor<TicketListQuery> query = ArgumentCaptor.forClass(TicketListQuery.class);
    verify(ticketService).list(query.capture());
    org.assertj.core.api.Assertions.assertThat(query.getValue().q()).isEqualTo("password");
    org.assertj.core.api.Assertions.assertThat(query.getValue().statuses())
        .containsExactly(TicketStatus.IN_PROGRESS);
  }

  @Test
  void dashboardRendersDistinctBadgeColorForEveryStatus() throws Exception {
    List<TicketEntity> tickets =
        List.of(
            ticket(1L, "Open", TicketStatus.OPEN, null),
            ticket(2L, "In progress", TicketStatus.IN_PROGRESS, null),
            ticket(3L, "Resolved", TicketStatus.RESOLVED, null),
            ticket(4L, "Closed", TicketStatus.CLOSED, null),
            ticket(5L, "Cancelled", TicketStatus.CANCELLED, null));
    when(ticketService.list(any(TicketListQuery.class)))
        .thenReturn(new PageImpl<>(tickets, PageRequest.of(0, 20), tickets.size()));

    mockMvc
        .perform(get("/tickets"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("bg-blue-50")))
        .andExpect(content().string(containsString("bg-amber-50")))
        .andExpect(content().string(containsString("bg-emerald-50")))
        .andExpect(content().string(containsString("bg-slate-100")))
        .andExpect(content().string(containsString("bg-red-50")));
  }

  @Test
  void newTicketRendersFormShell() throws Exception {
    mockMvc
        .perform(get("/tickets/new"))
        .andExpect(status().isOk())
        .andExpect(view().name("tickets/create"))
        .andExpect(model().attributeExists("ticketCreateForm", "priorities"))
        .andExpect(content().string(containsString("Create ticket")));
  }

  @Test
  void invalidCreateReRendersMeaningfulFieldErrors() throws Exception {
    mockMvc
        .perform(
            post("/tickets")
                .param("title", " ")
                .param("description", "")
                .param("priority", "")
                .param("assigneeId", "0"))
        .andExpect(status().isOk())
        .andExpect(view().name("tickets/create"))
        .andExpect(
            model()
                .attributeHasFieldErrors(
                    "ticketCreateForm", "title", "description", "priority", "assigneeId"))
        .andExpect(content().string(containsString("Title is required")))
        .andExpect(content().string(containsString("Description is required")))
        .andExpect(content().string(containsString("Priority is required")))
        .andExpect(content().string(containsString("Assignee must be a positive ID")));
  }

  @Test
  void validCreateRedirectsToTicketDetail() throws Exception {
    TicketEntity saved = ticket(55L, "Printer unavailable", TicketStatus.OPEN, 44L);
    when(ticketService.create(anyLong(), any(CreateTicketCommand.class))).thenReturn(saved);

    mockMvc
        .perform(
            post("/tickets")
                .param("title", " Printer unavailable ")
                .param("description", " Cannot print invoices ")
                .param("priority", "HIGH")
                .param("assigneeId", "44"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/tickets/55"))
        .andExpect(flash().attribute("successMessage", "Ticket created successfully"));

    ArgumentCaptor<CreateTicketCommand> command =
        ArgumentCaptor.forClass(CreateTicketCommand.class);
    verify(ticketService).create(eq(7L), command.capture());
    org.assertj.core.api.Assertions.assertThat(command.getValue().title())
        .isEqualTo("Printer unavailable");
    org.assertj.core.api.Assertions.assertThat(command.getValue().priority())
        .isEqualTo(TicketPriority.HIGH);
  }

  @Test
  void ticketDetailRendersDetailsAndOnlyAllowedStatusActions() throws Exception {
    TicketEntity ticket = ticket(42L, "VPN unavailable", TicketStatus.OPEN, 44L);
    when(ticketService.get(42L)).thenReturn(new TicketDetail(ticket, List.of()));

    mockMvc
        .perform(get("/tickets/42"))
        .andExpect(status().isOk())
        .andExpect(view().name("tickets/detail"))
        .andExpect(model().attribute("ticket", ticket))
        .andExpect(model().attributeExists("comments", "allowedStatuses", "ticketUpdateForm"))
        .andExpect(content().string(containsString("VPN unavailable")))
        .andExpect(content().string(containsString("User 44")))
        .andExpect(content().string(containsString("Move to IN PROGRESS")))
        .andExpect(content().string(containsString("Move to CANCELLED")))
        .andExpect(content().string(not(containsString("Move to RESOLVED"))));
  }

  @Test
  void invalidTransitionRedirectsWithGlobalErrorAlert() throws Exception {
    doThrow(new InvalidStateTransitionException(TicketStatus.CLOSED, TicketStatus.OPEN))
        .when(ticketService)
        .patch(eq(42L), any(TicketPatch.class));

    mockMvc
        .perform(post("/tickets/42/status").param("status", "OPEN"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/tickets/42"))
        .andExpect(
            flash().attribute(
                    "errorMessage", "Cannot transition ticket from CLOSED to OPEN"));
  }

  @Test
  void priorityAndAssigneeUpdateRedirectsBackToDetail() throws Exception {
    mockMvc
        .perform(
            post("/tickets/42/details")
                .param("priority", "HIGH")
                .param("assigneeId", "91"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/tickets/42"))
        .andExpect(flash().attribute("successMessage", "Ticket details updated"));

    ArgumentCaptor<TicketPatch> patch = ArgumentCaptor.forClass(TicketPatch.class);
    verify(ticketService).patch(eq(42L), patch.capture());
    org.assertj.core.api.Assertions.assertThat(patch.getValue().priority())
        .isEqualTo(TicketPriority.HIGH);
    org.assertj.core.api.Assertions.assertThat(patch.getValue().assigneeId()).isEqualTo(91L);
    org.assertj.core.api.Assertions.assertThat(patch.getValue().assigneeSpecified()).isTrue();
  }

  @Test
  void detailDisplaysRedirectFlashError() throws Exception {
    TicketEntity ticket = ticket(42L, "VPN unavailable", TicketStatus.CLOSED, 44L);
    when(ticketService.get(42L)).thenReturn(new TicketDetail(ticket, List.of()));

    mockMvc
        .perform(
            get("/tickets/42")
                .flashAttr("errorMessage", "Cannot transition ticket from CLOSED to OPEN"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Cannot transition ticket from CLOSED to OPEN")))
        .andExpect(content().string(containsString("terminal state")));
  }

  @Test
  void validCommentRedirectsBackToDetail() throws Exception {
    mockMvc
        .perform(post("/tickets/42/comments").param("body", " Investigating now "))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/tickets/42"))
        .andExpect(flash().attribute("successMessage", "Comment added"));

    verify(commentService).add(7L, 42L, "Investigating now");
  }

  private static TicketEntity ticket(
      long id, String title, TicketStatus status, Long assigneeId) {
    TicketEntity ticket = new TicketEntity();
    ticket.setId(id);
    ticket.setTitle(title);
    ticket.setDescription("Description");
    ticket.setStatus(status);
    ticket.setPriority(TicketPriority.MEDIUM);
    ticket.setAssigneeId(assigneeId);
    ticket.setReporterId(7L);
    return ticket;
  }
}
