package com.c2.stms.config;

import com.c2.stms.service.ActorContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ActorIdFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Actor-Id";

  private final ActorContext actorContext;

  public ActorIdFilter(ActorContext actorContext) {
    this.actorContext = actorContext;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String raw = request.getHeader(HEADER);
      if (raw != null && !raw.isBlank()) {
        try {
          long actorId = Long.parseLong(raw.trim());
          if (actorId > 0) {
            actorContext.setActorId(actorId);
          }
        } catch (NumberFormatException ignored) {
          // Controller requireActorId() maps missing/invalid actor to 401.
        }
      }
      filterChain.doFilter(request, response);
    } finally {
      actorContext.clear();
    }
  }
}
