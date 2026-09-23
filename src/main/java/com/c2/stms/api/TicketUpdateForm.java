package com.c2.stms.api;

import com.c2.stms.domain.TicketPriority;
import com.c2.stms.service.TicketPatch;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TicketUpdateForm(
    @NotNull(message = "Priority is required") TicketPriority priority,
    @Positive(message = "Assignee must be a positive ID") Long assigneeId) {

  public TicketPatch toPatch() {
    return new TicketPatch(null, null, priority, null, false, assigneeId, true, null);
  }
}
