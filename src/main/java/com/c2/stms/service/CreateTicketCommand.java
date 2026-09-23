package com.c2.stms.service;

import com.c2.stms.domain.TicketPriority;

public record CreateTicketCommand(
    String title, String description, TicketPriority priority, String category, Long assigneeId) {}
