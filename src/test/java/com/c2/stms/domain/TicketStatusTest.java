package com.c2.stms.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TicketStatusTest {

  @ParameterizedTest(name = "{0} -> {1} allowed={2}")
  @MethodSource("matrix")
  void canTransitionMatchesStateMachineMatrix(TicketStatus from, TicketStatus to, boolean allowed) {
    assertThat(from.canTransition(to)).isEqualTo(allowed);
  }

  @Test
  void nullTargetIsNotAllowed() {
    assertThat(TicketStatus.OPEN.canTransition(null)).isFalse();
  }

  @Test
  void valuesMatchSpec() {
    assertThat(TicketStatus.values())
        .containsExactly(
            TicketStatus.OPEN,
            TicketStatus.IN_PROGRESS,
            TicketStatus.RESOLVED,
            TicketStatus.CLOSED,
            TicketStatus.CANCELLED);
    assertThat(TicketPriority.values())
        .containsExactly(
            TicketPriority.LOW, TicketPriority.MEDIUM, TicketPriority.HIGH, TicketPriority.URGENT);
  }

  static Stream<Arguments> matrix() {
    List<Arguments> args = new ArrayList<>();
    for (TicketStatus from : TicketStatus.values()) {
      for (TicketStatus to : TicketStatus.values()) {
        args.add(Arguments.of(from, to, expected(from, to)));
      }
    }
    return args.stream();
  }

  private static boolean expected(TicketStatus from, TicketStatus to) {
    if (from == to) {
      return true;
    }
    return switch (from) {
      case OPEN -> to == TicketStatus.IN_PROGRESS || to == TicketStatus.CANCELLED;
      case IN_PROGRESS -> to == TicketStatus.RESOLVED || to == TicketStatus.CANCELLED;
      case RESOLVED -> to == TicketStatus.CLOSED;
      case CLOSED, CANCELLED -> false;
    };
  }
}
