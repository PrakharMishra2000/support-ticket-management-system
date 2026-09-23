package com.c2.stms.service;

import com.c2.stms.domain.UnauthenticatedException;
import org.springframework.stereotype.Component;

@Component
public class ActorContext {

  private static final ThreadLocal<Long> ACTOR_ID = new ThreadLocal<>();

  public void setActorId(long actorId) {
    ACTOR_ID.set(actorId);
  }

  public void clear() {
    ACTOR_ID.remove();
  }

  public long requireActorId() {
    Long actorId = ACTOR_ID.get();
    if (actorId == null || actorId <= 0) {
      throw new UnauthenticatedException();
    }
    return actorId;
  }
}
