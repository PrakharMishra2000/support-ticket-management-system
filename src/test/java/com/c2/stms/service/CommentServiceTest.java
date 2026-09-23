package com.c2.stms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.c2.stms.domain.TicketNotFoundException;
import com.c2.stms.domain.TicketStatus;
import com.c2.stms.infrastructure.CommentEntity;
import com.c2.stms.infrastructure.CommentRepository;
import com.c2.stms.infrastructure.TicketRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

  @Mock private TicketRepository tickets;
  @Mock private CommentRepository comments;
  private CommentService service;

  @BeforeEach
  void setUp() {
    service = new CommentService(tickets, comments);
  }

  @ParameterizedTest
  @EnumSource(
      value = TicketStatus.class,
      names = {"CLOSED", "CANCELLED", "OPEN"})
  void addAllowedOnAnyStatusIncludingTerminal(TicketStatus ignoredStatus) {
    when(tickets.existsById(1L)).thenReturn(true);
    when(comments.save(any(CommentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

    CommentEntity saved = service.add(7L, 1L, "  Note  ");

    assertThat(saved.getAuthorId()).isEqualTo(7L);
    assertThat(saved.getBody()).isEqualTo("Note");
    assertThat(saved.getTicketId()).isEqualTo(1L);
  }

  @Test
  void addMissingTicketThrowsAndDoesNotSave() {
    when(tickets.existsById(9L)).thenReturn(false);
    assertThatThrownBy(() -> service.add(7L, 9L, "Note"))
        .isInstanceOf(TicketNotFoundException.class);
    verify(comments, never()).save(any());
  }

  @Test
  void listMissingTicketThrows() {
    when(tickets.existsById(9L)).thenReturn(false);
    assertThatThrownBy(() -> service.list(9L, 0, 20)).isInstanceOf(TicketNotFoundException.class);
  }

  @Test
  void listEmptyPageWhenTicketExistsWithoutComments() {
    when(tickets.existsById(1L)).thenReturn(true);
    when(comments.findByTicketIdOrderByCreatedAtAsc(eq(1L), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    Page<CommentEntity> page = service.list(1L, null, null);

    assertThat(page.getContent()).isEmpty();
  }
}
