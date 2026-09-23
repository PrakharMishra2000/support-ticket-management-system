package com.c2.stms.service;

import com.c2.stms.domain.TicketNotFoundException;
import com.c2.stms.infrastructure.CommentEntity;
import com.c2.stms.infrastructure.CommentRepository;
import com.c2.stms.infrastructure.TicketRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

  private final TicketRepository tickets;
  private final CommentRepository comments;

  public CommentService(TicketRepository tickets, CommentRepository comments) {
    this.tickets = tickets;
    this.comments = comments;
  }

  @Transactional
  public CommentEntity add(long actorId, long ticketId, String body) {
    if (actorId <= 0) {
      throw new IllegalArgumentException("actorId must be positive");
    }
    if (!tickets.existsById(ticketId)) {
      throw new TicketNotFoundException(ticketId);
    }
    String trimmed = body == null ? "" : body.trim();
    if (trimmed.isEmpty() || trimmed.length() > 4000) {
      throw new IllegalArgumentException("body: must be 1-4000 characters");
    }
    CommentEntity entity = new CommentEntity();
    entity.setTicketId(ticketId);
    entity.setBody(trimmed);
    entity.setAuthorId(actorId);
    return comments.save(entity);
  }

  @Transactional(readOnly = true)
  public Page<CommentEntity> list(long ticketId, Integer page, Integer size) {
    if (!tickets.existsById(ticketId)) {
      throw new TicketNotFoundException(ticketId);
    }
    int resolvedPage = page == null ? TicketService.DEFAULT_PAGE : page;
    int resolvedSize = size == null ? TicketService.DEFAULT_SIZE : size;
    if (resolvedPage < 0) {
      throw new IllegalArgumentException("page: must be >= 0");
    }
    if (resolvedSize < 1 || resolvedSize > TicketService.MAX_SIZE) {
      throw new IllegalArgumentException("size: must be 1-100");
    }
    PageRequest pageable =
        PageRequest.of(resolvedPage, resolvedSize, Sort.by(Sort.Direction.ASC, "createdAt"));
    return comments.findByTicketIdOrderByCreatedAtAsc(ticketId, pageable);
  }
}
