package com.c2.stms.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateForm(
    @NotBlank(message = "Comment is required")
        @Size(max = 4000, message = "Comment must be at most 4000 characters")
        String body) {

  public CommentCreateForm {
    body = body == null ? null : body.trim();
  }

  public static CommentCreateForm empty() {
    return new CommentCreateForm(null);
  }
}
