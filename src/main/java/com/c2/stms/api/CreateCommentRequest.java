package com.c2.stms.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
    @NotBlank(message = "must be 1-4000 characters")
        @Size(min = 1, max = 4000, message = "must be 1-4000 characters")
        String body) {

  public CreateCommentRequest {
    body = body == null ? null : body.trim();
  }
}
