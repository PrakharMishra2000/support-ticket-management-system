package com.c2.stms.api;

import com.c2.stms.infrastructure.CommentEntity;
import java.time.Instant;

public record CommentResponse(Long id, Long ticketId, String body, Long authorId, Instant createdAt) {

  public static CommentResponse from(CommentEntity entity) {
    return new CommentResponse(
        entity.getId(),
        entity.getTicketId(),
        entity.getBody(),
        entity.getAuthorId(),
        entity.getCreatedAt());
  }
}
