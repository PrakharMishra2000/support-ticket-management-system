package com.c2.stms.domain;

public final class TicketNotFoundException extends RuntimeException {

  private final long ticketId;

  public TicketNotFoundException(long ticketId) {
    super("Ticket %d was not found".formatted(ticketId));
    this.ticketId = ticketId;
  }

  public long ticketId() {
    return ticketId;
  }
}
