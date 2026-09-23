package com.c2.stms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.c2.stms.domain.UnauthenticatedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ActorContextTest {

  private final ActorContext context = new ActorContext();

  @AfterEach
  void tearDown() {
    context.clear();
  }

  @Test
  void requireActorIdReturnsSetValue() {
    context.setActorId(7L);
    assertThat(context.requireActorId()).isEqualTo(7L);
  }

  @Test
  void requireActorIdFailsWhenMissing() {
    assertThatThrownBy(context::requireActorId).isInstanceOf(UnauthenticatedException.class);
  }
}
