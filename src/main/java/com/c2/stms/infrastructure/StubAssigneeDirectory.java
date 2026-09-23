package com.c2.stms.infrastructure;

import com.c2.stms.domain.AssigneeDirectory;
import org.springframework.stereotype.Component;

/**
 * No user table this revision. Every positive id is known except {@link #UNKNOWN_ASSIGNEE_ID}
 * so tests can trigger {@code UNKNOWN_ASSIGNEE}.
 */
@Component
public class StubAssigneeDirectory implements AssigneeDirectory {

  public static final long UNKNOWN_ASSIGNEE_ID = 999_999_999L;

  @Override
  public boolean exists(long id) {
    return id > 0 && id != UNKNOWN_ASSIGNEE_ID;
  }
}
