package com.c2.stms.service;

import com.c2.stms.domain.TicketPriority;
import com.c2.stms.domain.TicketStatus;
import java.time.Instant;
import java.util.List;

public record TicketListQuery(
    String q,
    List<TicketStatus> statuses,
    List<TicketPriority> priorities,
    Long assigneeId,
    boolean unassignedOnly,
    Long reporterId,
    String category,
    Instant createdFrom,
    Instant createdTo,
    Integer page,
    Integer size) {

  public static TicketListQuery empty() {
    return new TicketListQuery(null, List.of(), List.of(), null, false, null, null, null, null, 0, 20);
  }
}
