package com.c2.stms.api;

import com.c2.stms.domain.TicketPriority;
import com.c2.stms.domain.TicketStatus;
import com.c2.stms.service.CreateTicketCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateTicketRequest(
    @NotBlank(message = "must be 1-200 characters")
        @Size(min = 1, max = 200, message = "must be 1-200 characters")
        String title,
    @NotBlank(message = "must be 1-8000 characters")
        @Size(min = 1, max = 8000, message = "must be 1-8000 characters")
        String description,
    TicketPriority priority,
    @Size(max = 64, message = "must be at most 64 characters")
        @Pattern(regexp = "^[A-Za-z0-9 _-]+$", message = "invalid format")
        String category,
    @Positive(message = "must be a positive integer") Long assigneeId,
    @Null(message = "must not be provided") TicketStatus status,
    @Null(message = "must not be provided") Long id,
    @Null(message = "must not be provided") Long reporterId,
    @Null(message = "must not be provided") Instant createdAt) {

  public CreateTicketRequest {
    title = trimToNull(title);
    description = trimToNull(description);
    category = trimToNull(category);
  }

  public CreateTicketCommand toCommand() {
    return new CreateTicketCommand(title, description, priority, category, assigneeId);
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
