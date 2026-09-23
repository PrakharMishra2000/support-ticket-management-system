package com.c2.stms.infrastructure;

import com.c2.stms.service.TicketListQuery;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class TicketSpecifications {

  private TicketSpecifications() {}

  public static Specification<TicketEntity> matching(TicketListQuery query) {
    return (root, criteriaQuery, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (StringUtils.hasText(query.q())) {
        String pattern = containsPattern(query.q());
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("title")), pattern, '\\'),
                cb.like(cb.lower(root.get("description")), pattern, '\\')));
      }
      if (query.statuses() != null && !query.statuses().isEmpty()) {
        predicates.add(root.get("status").in(query.statuses()));
      }
      if (query.priorities() != null && !query.priorities().isEmpty()) {
        predicates.add(root.get("priority").in(query.priorities()));
      }
      if (query.unassignedOnly()) {
        predicates.add(cb.isNull(root.get("assigneeId")));
      } else if (query.assigneeId() != null) {
        predicates.add(cb.equal(root.get("assigneeId"), query.assigneeId()));
      }
      if (query.reporterId() != null) {
        predicates.add(cb.equal(root.get("reporterId"), query.reporterId()));
      }
      if (StringUtils.hasText(query.category())) {
        predicates.add(
            cb.equal(cb.lower(root.get("category")), query.category().trim().toLowerCase(Locale.ROOT)));
      }
      if (query.createdFrom() != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), query.createdFrom()));
      }
      if (query.createdTo() != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), query.createdTo()));
      }
      if (predicates.isEmpty()) {
        return cb.conjunction();
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  static String containsPattern(String q) {
    String escaped =
        q.trim()
            .toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    return "%" + escaped + "%";
  }
}
