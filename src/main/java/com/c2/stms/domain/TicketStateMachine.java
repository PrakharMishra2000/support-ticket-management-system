package com.c2.stms.domain;

import java.util.Objects;

/**
 * Isolated ticket status validator. Persistence and HTTP must call this rather than
 * assigning {@link TicketStatus} directly.
 */
public final class TicketStateMachine {

  public TicketStatus apply(TicketStatus current, TicketStatus target) {
    Objects.requireNonNull(current, "current");
    Objects.requireNonNull(target, "target");
    if (current == target) {
      return current;
    }
    if (!current.canTransition(target)) {
      throw new InvalidStateTransitionException(current, target);
    }
    return target;
  }
}
