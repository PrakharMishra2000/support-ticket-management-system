package com.c2.stms.api;

import com.c2.stms.domain.TicketPriority;
import com.c2.stms.domain.TicketStatus;
import com.c2.stms.service.TicketPatch;
import tools.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

@JsonDeserialize(using = PatchTicketRequestDeserializer.class)
public record PatchTicketRequest(
    @Size(min = 1, max = 200, message = "must be 1-200 characters") String title,
    @Size(min = 1, max = 8000, message = "must be 1-8000 characters") String description,
    TicketPriority priority,
    @Size(max = 64, message = "must be at most 64 characters")
        @Pattern(regexp = "^[A-Za-z0-9 _-]+$", message = "invalid format")
        String category,
    boolean categorySpecified,
    @Positive(message = "must be a positive integer") Long assigneeId,
    boolean assigneeSpecified,
    TicketStatus status,
    @Null(message = "must not be provided") Long id,
    @Null(message = "must not be provided") Long reporterId,
    @Null(message = "must not be provided") Instant createdAt) {

  @AssertTrue(message = "at least one writable field is required")
  public boolean hasWritableField() {
    return title != null
        || description != null
        || priority != null
        || categorySpecified
        || assigneeSpecified
        || status != null;
  }

  public TicketPatch toPatch() {
    return new TicketPatch(
        title, description, priority, category, categorySpecified, assigneeId, assigneeSpecified, status);
  }
}
