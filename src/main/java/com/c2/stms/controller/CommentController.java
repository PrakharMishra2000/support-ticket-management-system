package com.c2.stms.controller;

import com.c2.stms.api.CommentResponse;
import com.c2.stms.api.CreateCommentRequest;
import com.c2.stms.api.PageResponse;
import com.c2.stms.infrastructure.CommentEntity;
import com.c2.stms.service.ActorContext;
import com.c2.stms.service.CommentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/tickets/{ticketId}/comments")
public class CommentController {

  private final CommentService comments;
  private final ActorContext actors;

  public CommentController(CommentService comments, ActorContext actors) {
    this.comments = comments;
    this.actors = actors;
  }

  @PostMapping
  public ResponseEntity<CommentResponse> add(
      @PathVariable long ticketId, @Valid @RequestBody CreateCommentRequest request) {
    var saved = comments.add(actors.requireActorId(), ticketId, request.body());
    return ResponseEntity.created(
            URI.create("/api/v1/tickets/" + ticketId + "/comments/" + saved.getId()))
        .body(CommentResponse.from(saved));
  }

  @GetMapping
  public PageResponse<CommentResponse> list(
      @PathVariable long ticketId,
      @RequestParam(required = false) @Min(0) Integer page,
      @RequestParam(required = false) Integer size) {
    Page<CommentEntity> result = comments.list(ticketId, page, size);
    return new PageResponse<>(
        result.getContent().stream().map(CommentResponse::from).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }
}
