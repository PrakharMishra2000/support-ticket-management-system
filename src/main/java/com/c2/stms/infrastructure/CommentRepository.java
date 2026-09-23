package com.c2.stms.infrastructure;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

  List<CommentEntity> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

  Page<CommentEntity> findByTicketIdOrderByCreatedAtAsc(Long ticketId, Pageable pageable);
}
