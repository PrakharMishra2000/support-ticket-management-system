package com.c2.stms.service;

import com.c2.stms.domain.TicketPriority;
import com.c2.stms.domain.TicketStatus;

/**
 * {@code *Specified} flags distinguish omitted JSON fields from explicit {@code null} clears.
 */
public record TicketPatch(
    String title,
    String description,
    TicketPriority priority,
    String category,
    boolean categorySpecified,
    Long assigneeId,
    boolean assigneeSpecified,
    TicketStatus status) {}
