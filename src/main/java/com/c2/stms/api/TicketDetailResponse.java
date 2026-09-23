package com.c2.stms.api;

import com.c2.stms.domain.TicketPriority;
import com.c2.stms.domain.TicketStatus;
import com.c2.stms.service.TicketDetail;
import java.time.Instant;
import java.util.List;

public record TicketDetailResponse(
    Long id,
    String title,
    String description,
    TicketStatus status,
    TicketPriority priority,
    String category,
    Long assigneeId,
    Long reporterId,
    Instant createdAt,
    Instant updatedAt,
    List<CommentResponse> comments) {

  public static TicketDetailResponse from(TicketDetail detail) {
    var ticket = detail.ticket();
    return new TicketDetailResponse(
        ticket.getId(),
        ticket.getTitle(),
        ticket.getDescription(),
        ticket.getStatus(),
        ticket.getPriority(),
        ticket.getCategory(),
        ticket.getAssigneeId(),
        ticket.getReporterId(),
        ticket.getCreatedAt(),
        ticket.getUpdatedAt(),
        detail.comments().stream().map(CommentResponse::from).toList());
  }
}
