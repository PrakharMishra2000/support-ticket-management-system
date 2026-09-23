package com.c2.stms.api;

import com.c2.stms.domain.TicketPriority;
import com.c2.stms.domain.TicketStatus;
import com.c2.stms.infrastructure.TicketEntity;
import java.time.Instant;

public record TicketResponse(
    Long id,
    String title,
    String description,
    TicketStatus status,
    TicketPriority priority,
    String category,
    Long assigneeId,
    Long reporterId,
    Instant createdAt,
    Instant updatedAt) {

  public static TicketResponse from(TicketEntity entity) {
    return new TicketResponse(
        entity.getId(),
        entity.getTitle(),
        entity.getDescription(),
        entity.getStatus(),
        entity.getPriority(),
        entity.getCategory(),
        entity.getAssigneeId(),
        entity.getReporterId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
