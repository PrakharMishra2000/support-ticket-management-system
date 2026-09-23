package com.c2.stms.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Ticket lifecycle statuses. Transition matrix is defined here; enforcement lives in
 * {@link TicketStateMachine}.
 */
public enum TicketStatus {
  OPEN,
  IN_PROGRESS,
  RESOLVED,
  CLOSED,
  CANCELLED;

  private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED;

  static {
    EnumMap<TicketStatus, Set<TicketStatus>> allowed = new EnumMap<>(TicketStatus.class);
    allowed.put(OPEN, Collections.unmodifiableSet(EnumSet.of(IN_PROGRESS, CANCELLED)));
    allowed.put(IN_PROGRESS, Collections.unmodifiableSet(EnumSet.of(RESOLVED, CANCELLED)));
    allowed.put(RESOLVED, Collections.unmodifiableSet(EnumSet.of(CLOSED)));
    allowed.put(CLOSED, Collections.unmodifiableSet(EnumSet.noneOf(TicketStatus.class)));
    allowed.put(CANCELLED, Collections.unmodifiableSet(EnumSet.noneOf(TicketStatus.class)));
    ALLOWED = Collections.unmodifiableMap(allowed);
  }

  /**
   * Same status is a no-op ({@code true}). Only edges in spec/state-machine.md are otherwise allowed.
   */
  public boolean canTransition(TicketStatus target) {
    if (target == null) {
      return false;
    }
    if (this == target) {
      return true;
    }
    return ALLOWED.get(this).contains(target);
  }

  public Set<TicketStatus> allowedTargets() {
    return ALLOWED.get(this);
  }

  public boolean isTerminal() {
    return this == CLOSED || this == CANCELLED;
  }
}
