package com.c2.stms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.c2.stms.domain.AssigneeDirectory;
import com.c2.stms.domain.InvalidStateTransitionException;
import com.c2.stms.domain.TicketNotFoundException;
import com.c2.stms.domain.TicketPriority;
import com.c2.stms.domain.TicketStateMachine;
import com.c2.stms.domain.TicketStatus;
import com.c2.stms.domain.UnknownAssigneeException;
import com.c2.stms.infrastructure.CommentRepository;
import com.c2.stms.infrastructure.StubAssigneeDirectory;
import com.c2.stms.infrastructure.TicketEntity;
import com.c2.stms.infrastructure.TicketRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

  @Mock private TicketRepository tickets;
  @Mock private CommentRepository comments;
  private TicketService service;

  @BeforeEach
  void setUp() {
    AssigneeDirectory assignees = new StubAssigneeDirectory();
    service = new TicketService(tickets, comments, assignees, new TicketStateMachine());
  }

  @Test
  void createSetsOpenStatusAndReporter() {
    when(tickets.save(any(TicketEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

    TicketEntity saved =
        service.create(7L, new CreateTicketCommand("  Title  ", " Body ", null, null, 44L));

    assertThat(saved.getStatus()).isEqualTo(TicketStatus.OPEN);
    assertThat(saved.getReporterId()).isEqualTo(7L);
    assertThat(saved.getPriority()).isEqualTo(TicketPriority.MEDIUM);
    assertThat(saved.getTitle()).isEqualTo("Title");
    assertThat(saved.getAssigneeId()).isEqualTo(44L);
  }

  @Test
  void createDoesNotSaveWhenTitleBlank() {
    assertThatThrownBy(
            () -> service.create(7L, new CreateTicketCommand("  ", "Body", null, null, null)))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tickets, never()).save(any());
  }

  @Test
  void createRejectsUnknownAssignee() {
    assertThatThrownBy(
            () ->
                service.create(
                    7L,
                    new CreateTicketCommand(
                        "Title", "Body", null, null, StubAssigneeDirectory.UNKNOWN_ASSIGNEE_ID)))
        .isInstanceOf(UnknownAssigneeException.class);
    verify(tickets, never()).save(any());
  }

  @Test
  void getMissingTicketThrows() {
    when(tickets.findById(9L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.get(9L)).isInstanceOf(TicketNotFoundException.class);
  }

  @Test
  void getLoadsCommentsOldestFirstFromRepository() {
    TicketEntity entity = openTicket();
    when(tickets.findById(1L)).thenReturn(Optional.of(entity));
    when(comments.findByTicketIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

    TicketDetail detail = service.get(1L);

    assertThat(detail.ticket()).isSameAs(entity);
    verify(comments).findByTicketIdOrderByCreatedAtAsc(1L);
  }

  @Test
  void patchClearsAssigneeWhenSpecifiedNull() {
    TicketEntity entity = openTicket();
    entity.setAssigneeId(44L);
    when(tickets.findById(1L)).thenReturn(Optional.of(entity));
    when(tickets.save(any(TicketEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

    TicketEntity patched =
        service.patch(
            1L, new TicketPatch(null, null, null, null, false, null, true, null));

    assertThat(patched.getAssigneeId()).isNull();
  }

  @Test
  void patchAllowedTransitionPersistsNewStatus() {
    TicketEntity entity = openTicket();
    when(tickets.findById(1L)).thenReturn(Optional.of(entity));
    when(tickets.save(any(TicketEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

    TicketEntity patched =
        service.patch(
            1L, new TicketPatch(null, null, null, null, false, null, false, TicketStatus.IN_PROGRESS));

    assertThat(patched.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    verify(tickets).save(entity);
  }

  @Test
  void patchIllegalTransitionDoesNotSave() {
    TicketEntity entity = openTicket();
    when(tickets.findById(1L)).thenReturn(Optional.of(entity));

    assertThatThrownBy(
            () ->
                service.patch(
                    1L,
                    new TicketPatch(
                        "New title", null, null, null, false, null, false, TicketStatus.CLOSED)))
        .isInstanceOf(InvalidStateTransitionException.class);

    verify(tickets, never()).save(any());
    assertThat(entity.getStatus()).isEqualTo(TicketStatus.OPEN);
  }

  @Test
  void listUsesDescendingCreatedAtAndEmptyPageForUnknownAssigneeFilter() {
    when(tickets.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(Page.empty());

    Page<TicketEntity> page =
        service.list(
            new TicketListQuery(
                null, List.of(), List.of(), 4242L, false, null, null, null, null, 0, 20));

    assertThat(page.getContent()).isEmpty();
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(tickets).findAll(any(Specification.class), pageable.capture());
    assertThat(pageable.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
  }

  @Test
  void listRejectsOversizedPage() {
    assertThatThrownBy(
            () ->
                service.list(
                    new TicketListQuery(
                        null, List.of(), List.of(), null, false, null, null, null, null, 0, 101)))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tickets, never()).findAll(any(Specification.class), any(Pageable.class));
  }

  private static TicketEntity openTicket() {
    TicketEntity entity = new TicketEntity();
    entity.setTitle("Title");
    entity.setDescription("Body");
    entity.setStatus(TicketStatus.OPEN);
    entity.setPriority(TicketPriority.MEDIUM);
    entity.setReporterId(7L);
    entity.setAssigneeId(44L);
    return entity;
  }
}
