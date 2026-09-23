package com.c2.stms.service;

import com.c2.stms.domain.AssigneeDirectory;
import com.c2.stms.domain.TicketNotFoundException;
import com.c2.stms.domain.TicketPriority;
import com.c2.stms.domain.TicketStateMachine;
import com.c2.stms.domain.TicketStatus;
import com.c2.stms.domain.UnknownAssigneeException;
import com.c2.stms.infrastructure.CommentEntity;
import com.c2.stms.infrastructure.CommentRepository;
import com.c2.stms.infrastructure.TicketEntity;
import com.c2.stms.infrastructure.TicketRepository;
import com.c2.stms.infrastructure.TicketSpecifications;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

  static final int DEFAULT_PAGE = 0;
  static final int DEFAULT_SIZE = 20;
  static final int MAX_SIZE = 100;
  private static final Pattern CATEGORY = Pattern.compile("^[A-Za-z0-9 _-]{1,64}$");

  private final TicketRepository tickets;
  private final CommentRepository comments;
  private final AssigneeDirectory assignees;
  private final TicketStateMachine stateMachine;

  public TicketService(
      TicketRepository tickets,
      CommentRepository comments,
      AssigneeDirectory assignees,
      TicketStateMachine stateMachine) {
    this.tickets = tickets;
    this.comments = comments;
    this.assignees = assignees;
    this.stateMachine = stateMachine;
  }

  @Transactional
  public TicketEntity create(long actorId, CreateTicketCommand command) {
    requirePositiveActor(actorId);
    String title = requireText(command.title(), "title", 1, 200);
    String description = requireText(command.description(), "description", 1, 8000);
    String category = optionalCategory(command.category());
    Long assigneeId = optionalAssignee(command.assigneeId());
    TicketPriority priority = command.priority() == null ? TicketPriority.MEDIUM : command.priority();

    TicketEntity entity = new TicketEntity();
    entity.setTitle(title);
    entity.setDescription(description);
    entity.setCategory(category);
    entity.setAssigneeId(assigneeId);
    entity.setPriority(priority);
    entity.setStatus(TicketStatus.OPEN);
    entity.setReporterId(actorId);

    return tickets.save(entity);
  }

  @Transactional(readOnly = true)
  public TicketDetail get(long id) {
    TicketEntity ticket =
        tickets.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
    List<CommentEntity> ticketComments = comments.findByTicketIdOrderByCreatedAtAsc(id);
    return new TicketDetail(ticket, ticketComments);
  }

  @Transactional
  public TicketEntity patch(long id, TicketPatch patch) {
    if (!hasWritableField(patch)) {
      throw new IllegalArgumentException("At least one writable field is required");
    }
    TicketEntity entity =
        tickets.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
    if (patch.status() != null) {
      entity.setStatus(stateMachine.apply(entity.getStatus(), patch.status()));
    }
    if (patch.title() != null) {
      entity.setTitle(requireText(patch.title(), "title", 1, 200));
    }
    if (patch.description() != null) {
      entity.setDescription(requireText(patch.description(), "description", 1, 8000));
    }
    if (patch.priority() != null) {
      entity.setPriority(patch.priority());
    }
    if (patch.categorySpecified()) {
      entity.setCategory(optionalCategory(patch.category()));
    }
    if (patch.assigneeSpecified()) {
      entity.setAssigneeId(optionalAssignee(patch.assigneeId()));
    }
    return tickets.save(entity);
  }

  @Transactional(readOnly = true)
  public Page<TicketEntity> list(TicketListQuery query) {
    TicketListQuery safe = query == null ? TicketListQuery.empty() : query;
    validateListQuery(safe);
    int page = safe.page() == null ? DEFAULT_PAGE : safe.page();
    int size = safe.size() == null ? DEFAULT_SIZE : safe.size();
    PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    return tickets.findAll(TicketSpecifications.matching(safe), pageable);
  }

  private static boolean hasWritableField(TicketPatch patch) {
    return patch.title() != null
        || patch.description() != null
        || patch.priority() != null
        || patch.categorySpecified()
        || patch.assigneeSpecified()
        || patch.status() != null;
  }

  private static void requirePositiveActor(long actorId) {
    if (actorId <= 0) {
      throw new IllegalArgumentException("actorId must be positive");
    }
  }

  private static String requireText(String value, String field, int min, int max) {
    if (value == null) {
      throw new IllegalArgumentException(field + ": must be " + min + "-" + max + " characters");
    }
    String trimmed = value.trim();
    if (trimmed.length() < min || trimmed.length() > max) {
      throw new IllegalArgumentException(field + ": must be " + min + "-" + max + " characters");
    }
    return trimmed;
  }

  private static String optionalCategory(String category) {
    if (category == null) {
      return null;
    }
    String trimmed = category.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    if (!CATEGORY.matcher(trimmed).matches()) {
      throw new IllegalArgumentException("category: invalid format");
    }
    return trimmed;
  }

  private Long optionalAssignee(Long assigneeId) {
    if (assigneeId == null) {
      return null;
    }
    if (assigneeId <= 0 || !assignees.exists(assigneeId)) {
      throw new UnknownAssigneeException(assigneeId);
    }
    return assigneeId;
  }

  private static void validateListQuery(TicketListQuery query) {
    if (query.page() != null && query.page() < 0) {
      throw new IllegalArgumentException("page: must be >= 0");
    }
    if (query.size() != null && (query.size() < 1 || query.size() > MAX_SIZE)) {
      throw new IllegalArgumentException("size: must be 1-100");
    }
    if (query.q() != null && !query.q().isBlank()) {
      String q = query.q().trim();
      if (q.length() > 200 || q.chars().anyMatch(Character::isISOControl)) {
        throw new IllegalArgumentException("q: invalid search text");
      }
    }
    if (query.createdFrom() != null
        && query.createdTo() != null
        && query.createdFrom().isAfter(query.createdTo())) {
      throw new IllegalArgumentException("createdFrom: must be <= createdTo");
    }
  }
}
