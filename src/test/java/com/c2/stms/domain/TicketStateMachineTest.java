package com.c2.stms.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TicketStateMachineTest {

  private TicketStateMachine stateMachine;

  @BeforeEach
  void setUp() {
    stateMachine = new TicketStateMachine();
  }

  @ParameterizedTest
  @CsvSource({
    "OPEN, IN_PROGRESS",
    "OPEN, CANCELLED",
    "IN_PROGRESS, RESOLVED",
    "IN_PROGRESS, CANCELLED",
    "RESOLVED, CLOSED"
  })
  void applyAllowsDocumentedEdges(TicketStatus from, TicketStatus to) {
    assertThat(stateMachine.apply(from, to)).isEqualTo(to);
  }

  @ParameterizedTest
  @CsvSource({"OPEN, OPEN", "CLOSED, CLOSED", "CANCELLED, CANCELLED"})
  void applySameStatusIsNoOp(TicketStatus status, TicketStatus same) {
    assertThat(stateMachine.apply(status, same)).isEqualTo(status);
  }

  @ParameterizedTest
  @CsvSource({
    "CLOSED, OPEN",
    "RESOLVED, OPEN",
    "CANCELLED, OPEN",
    "OPEN, RESOLVED",
    "OPEN, CLOSED",
    "IN_PROGRESS, CLOSED",
    "IN_PROGRESS, OPEN",
    "RESOLVED, CANCELLED"
  })
  void applyRejectsForbiddenTransitionsWithoutReturningNewStatus(
      TicketStatus from, TicketStatus to) {
    assertThatThrownBy(() -> stateMachine.apply(from, to))
        .isInstanceOf(InvalidStateTransitionException.class)
        .satisfies(
            ex -> {
              InvalidStateTransitionException invalid = (InvalidStateTransitionException) ex;
              assertThat(invalid.from()).isEqualTo(from);
              assertThat(invalid.to()).isEqualTo(to);
            });
  }

  @Test
  void applyRequiresBothStatuses() {
    assertThatThrownBy(() -> stateMachine.apply(null, TicketStatus.OPEN))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> stateMachine.apply(TicketStatus.OPEN, null))
        .isInstanceOf(NullPointerException.class);
  }
}
