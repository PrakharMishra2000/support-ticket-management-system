package com.c2.stms.service;

import com.c2.stms.infrastructure.CommentEntity;
import com.c2.stms.infrastructure.TicketEntity;
import java.util.List;

public record TicketDetail(TicketEntity ticket, List<CommentEntity> comments) {}
