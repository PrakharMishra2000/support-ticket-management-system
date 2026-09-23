package com.c2.stms.controller;

import com.c2.stms.api.CreateTicketRequest;
import com.c2.stms.api.PageResponse;
import com.c2.stms.api.PatchTicketRequest;
import com.c2.stms.api.TicketDetailResponse;
import com.c2.stms.api.TicketResponse;
import com.c2.stms.domain.TicketPriority;
import com.c2.stms.domain.TicketStatus;
import com.c2.stms.infrastructure.TicketEntity;
import com.c2.stms.service.ActorContext;
import com.c2.stms.service.TicketListQuery;
import com.c2.stms.service.TicketService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/tickets")
public class TicketController {

  private final TicketService tickets;
  private final ActorContext actors;

  public TicketController(TicketService tickets, ActorContext actors) {
    this.tickets = tickets;
    this.actors = actors;
  }

  @PostMapping
  public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
    var saved = tickets.create(actors.requireActorId(), request.toCommand());
    return ResponseEntity.created(URI.create("/api/v1/tickets/" + saved.getId()))
        .body(TicketResponse.from(saved));
  }

  @GetMapping
  public PageResponse<TicketResponse> list(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) List<TicketStatus> status,
      @RequestParam(required = false) List<TicketPriority> priority,
      @RequestParam(required = false) String assigneeId,
      @RequestParam(required = false) Long reporterId,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) Instant createdFrom,
      @RequestParam(required = false) Instant createdTo,
      @RequestParam(required = false) @Min(0) Integer page,
      @RequestParam(required = false) Integer size) {
    TicketListQuery query =
        new TicketListQuery(
            q,
            status == null ? List.of() : status,
            priority == null ? List.of() : priority,
            parseAssigneeId(assigneeId),
            "none".equalsIgnoreCase(assigneeId),
            reporterId,
            category,
            createdFrom,
            createdTo,
            page,
            size);
    Page<TicketEntity> result = tickets.list(query);
    return new PageResponse<>(
        result.getContent().stream().map(TicketResponse::from).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @GetMapping("/{id}")
  public TicketDetailResponse get(@PathVariable long id) {
    return TicketDetailResponse.from(tickets.get(id));
  }

  @PatchMapping("/{id}")
  public TicketResponse patch(
      @PathVariable long id, @Valid @RequestBody PatchTicketRequest request) {
    return TicketResponse.from(tickets.patch(id, request.toPatch()));
  }

  private static Long parseAssigneeId(String assigneeId) {
    if (assigneeId == null || assigneeId.isBlank() || "none".equalsIgnoreCase(assigneeId)) {
      return null;
    }
    try {
      long value = Long.parseLong(assigneeId);
      if (value <= 0) {
        throw new IllegalArgumentException("assigneeId: must be a positive integer or none");
      }
      return value;
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("assigneeId: must be a positive integer or none");
    }
  }
}
