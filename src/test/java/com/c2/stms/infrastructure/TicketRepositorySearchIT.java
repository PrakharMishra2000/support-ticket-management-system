package com.c2.stms.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.c2.stms.domain.TicketPriority;
import com.c2.stms.domain.TicketStatus;
import com.c2.stms.service.TicketListQuery;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TicketRepositorySearchIT {

  @Autowired private TicketRepository tickets;

  @BeforeEach
  void seed() {
    tickets.save(ticket("Password reset", "Email never arrives", TicketStatus.OPEN, null));
    tickets.save(ticket("VPN down", "Cannot reach office", TicketStatus.IN_PROGRESS, 44L));
    tickets.save(ticket("Laptop request", "Need a charger", TicketStatus.CANCELLED, null));
  }

  @Test
  void keywordSearchMatchesTitleOrDescription() {
    TicketListQuery query =
        new TicketListQuery(
            "password", List.of(), List.of(), null, false, null, null, null, null, 0, 20);

    Page<TicketEntity> page = tickets.findAll(TicketSpecifications.matching(query), pageable());

    assertThat(page.getContent()).extracting(TicketEntity::getTitle).containsExactly("Password reset");
  }

  @Test
  void statusFilterReturnsOnlyRequestedStatuses() {
    TicketListQuery query =
        new TicketListQuery(
            null,
            List.of(TicketStatus.CANCELLED),
            List.of(),
            null,
            false,
            null,
            null,
            null,
            null,
            0,
            20);

    Page<TicketEntity> page = tickets.findAll(TicketSpecifications.matching(query), pageable());

    assertThat(page.getContent())
        .extracting(TicketEntity::getStatus)
        .containsExactly(TicketStatus.CANCELLED);
  }

  @Test
  void keywordAndStatusAreAnded() {
    TicketListQuery query =
        new TicketListQuery(
            "reach",
            List.of(TicketStatus.OPEN),
            List.of(),
            null,
            false,
            null,
            null,
            null,
            null,
            0,
            20);

    Page<TicketEntity> page = tickets.findAll(TicketSpecifications.matching(query), pageable());

    assertThat(page.getContent()).isEmpty();
  }

  @Test
  void unassignedFilter() {
    TicketListQuery query =
        new TicketListQuery(null, List.of(), List.of(), null, true, null, null, null, null, 0, 20);

    Page<TicketEntity> page = tickets.findAll(TicketSpecifications.matching(query), pageable());

    assertThat(page.getContent()).allMatch(ticket -> ticket.getAssigneeId() == null);
    assertThat(page.getContent()).hasSize(2);
  }

  private static PageRequest pageable() {
    return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
  }

  private static TicketEntity ticket(
      String title, String description, TicketStatus status, Long assigneeId) {
    TicketEntity entity = new TicketEntity();
    entity.setTitle(title);
    entity.setDescription(description);
    entity.setStatus(status);
    entity.setPriority(TicketPriority.MEDIUM);
    entity.setAssigneeId(assigneeId);
    entity.setReporterId(7L);
    return entity;
  }
}
