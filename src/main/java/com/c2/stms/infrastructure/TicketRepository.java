package com.c2.stms.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TicketRepository
    extends JpaRepository<TicketEntity, Long>, JpaSpecificationExecutor<TicketEntity> {}
