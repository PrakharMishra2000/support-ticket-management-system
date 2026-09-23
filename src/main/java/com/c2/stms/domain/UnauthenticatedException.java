package com.c2.stms.domain;

public final class UnauthenticatedException extends RuntimeException {

  public UnauthenticatedException() {
    super("Authenticated actor is required");
  }
}
