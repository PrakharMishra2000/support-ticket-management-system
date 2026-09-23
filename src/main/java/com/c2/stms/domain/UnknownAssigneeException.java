package com.c2.stms.domain;

public final class UnknownAssigneeException extends RuntimeException {

  private final long assigneeId;

  public UnknownAssigneeException(long assigneeId) {
    super("Unknown assignee %d".formatted(assigneeId));
    this.assigneeId = assigneeId;
  }

  public long assigneeId() {
    return assigneeId;
  }
}
