package com.c2.stms.domain;

public final class InvalidStateTransitionException extends RuntimeException {

  private final TicketStatus from;
  private final TicketStatus to;

  public InvalidStateTransitionException(TicketStatus from, TicketStatus to) {
    super("Cannot transition ticket from %s to %s".formatted(from, to));
    this.from = from;
    this.to = to;
  }

  public TicketStatus from() {
    return from;
  }

  public TicketStatus to() {
    return to;
  }
}
