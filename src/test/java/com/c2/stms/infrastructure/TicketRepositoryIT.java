package com.c2.stms.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.c2.stms.domain.TicketPriority;
import com.c2.stms.domain.TicketStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TicketRepositoryIT {

  @Autowired private TicketRepository tickets;

  @Test
  void saveAndFindRoundTrip() {
    TicketEntity entity = new TicketEntity();
    entity.setTitle("Cannot login");
    entity.setDescription("SSO redirect loops");
    entity.setStatus(TicketStatus.OPEN);
    entity.setPriority(TicketPriority.HIGH);
    entity.setReporterId(7L);

    TicketEntity saved = tickets.save(entity);

    assertThat(saved.getId()).isPositive();
    assertThat(tickets.findById(saved.getId()))
        .get()
        .extracting(TicketEntity::getStatus, TicketEntity::getPriority, TicketEntity::getReporterId)
        .containsExactly(TicketStatus.OPEN, TicketPriority.HIGH, 7L);
  }
}
