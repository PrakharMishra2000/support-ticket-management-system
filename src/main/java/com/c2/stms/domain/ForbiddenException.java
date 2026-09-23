package com.c2.stms.domain;

public final class ForbiddenException extends RuntimeException {

  public ForbiddenException() {
    super("Forbidden");
  }
}
