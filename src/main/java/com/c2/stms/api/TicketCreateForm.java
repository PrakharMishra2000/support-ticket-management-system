package com.c2.stms.api;

import com.c2.stms.domain.TicketPriority;
import com.c2.stms.service.CreateTicketCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TicketCreateForm(
    @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,
    @NotBlank(message = "Description is required")
        @Size(max = 8000, message = "Description must be at most 8000 characters")
        String description,
    @NotNull(message = "Priority is required") TicketPriority priority,
    @Positive(message = "Assignee must be a positive ID") Long assigneeId) {

  public TicketCreateForm {
    title = trim(title);
    description = trim(description);
  }

  public static TicketCreateForm empty() {
    return new TicketCreateForm(null, null, TicketPriority.MEDIUM, null);
  }

  public CreateTicketCommand toCommand() {
    return new CreateTicketCommand(title, description, priority, null, assigneeId);
  }

  private static String trim(String value) {
    return value == null ? null : value.trim();
  }
}
