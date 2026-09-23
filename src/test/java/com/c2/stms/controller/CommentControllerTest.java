package com.c2.stms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c2.stms.domain.TicketNotFoundException;
import com.c2.stms.infrastructure.CommentEntity;
import com.c2.stms.service.ActorContext;
import com.c2.stms.service.CommentService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CommentController.class)
@Import(GlobalExceptionHandler.class)
class CommentControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CommentService comments;
  @MockitoBean private ActorContext actors;

  @BeforeEach
  void actor() {
    when(actors.requireActorId()).thenReturn(7L);
  }

  @Test
  void addReturns201AndLocation() throws Exception {
    CommentEntity saved = new CommentEntity();
    saved.setId(3L);
    saved.setTicketId(12L);
    saved.setBody("Tried a second mailbox.");
    saved.setAuthorId(7L);
    when(comments.add(eq(7L), eq(12L), any())).thenReturn(saved);

    mockMvc
        .perform(
            post("/api/v1/tickets/12/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"body":"Tried a second mailbox."}
                    """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/tickets/12/comments/3"))
        .andExpect(jsonPath("$.id").value(3));
  }

  @Test
  void addBlankBodyReturns400() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/tickets/12/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"body":"  "}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void listEmptyReturns200() throws Exception {
    when(comments.list(12L, null, null))
        .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

    mockMvc
        .perform(get("/api/v1/tickets/12/comments"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty());
  }

  @Test
  void listMissingTicketReturns404() throws Exception {
    when(comments.list(eq(99L), any(), any())).thenThrow(new TicketNotFoundException(99L));

    mockMvc
        .perform(get("/api/v1/tickets/99/comments"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
  }
}
