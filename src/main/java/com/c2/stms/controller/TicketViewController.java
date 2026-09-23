package com.c2.stms.controller;

import com.c2.stms.api.CommentCreateForm;
import com.c2.stms.api.TicketCreateForm;
import com.c2.stms.api.TicketResponse;
import com.c2.stms.api.TicketUpdateForm;
import com.c2.stms.domain.InvalidStateTransitionException;
import com.c2.stms.domain.TicketPriority;
import com.c2.stms.domain.TicketStatus;
import com.c2.stms.domain.UnknownAssigneeException;
import com.c2.stms.service.CommentService;
import com.c2.stms.service.TicketListQuery;
import com.c2.stms.service.TicketPatch;
import com.c2.stms.service.TicketService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Routes server-rendered ticket pages. Ticket data continues to use the /api/v1 contract. */
@Controller
public class TicketViewController {

  private final TicketService ticketService;
  private final CommentService commentService;
  private final long viewActorId;

  public TicketViewController(
      TicketService ticketService,
      CommentService commentService,
      @Value("${stms.views.actor-id}") long viewActorId) {
    if (viewActorId <= 0) {
      throw new IllegalArgumentException("stms.views.actor-id must be positive");
    }
    this.ticketService = ticketService;
    this.commentService = commentService;
    this.viewActorId = viewActorId;
  }

  @GetMapping("/")
  public String home() {
    return "redirect:/tickets";
  }

  @GetMapping("/tickets")
  public String tickets(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) TicketStatus status,
      Model model) {
    String query = q == null ? "" : q.trim();
    var filters =
        new TicketListQuery(
            query,
            status == null ? List.of() : List.of(status),
            List.of(),
            null,
            false,
            null,
            null,
            null,
            null,
            0,
            20);
    var ticketPage = ticketService.list(filters);

    model.addAttribute(
        "tickets", ticketPage.getContent().stream().map(TicketResponse::from).toList());
    model.addAttribute("totalTickets", ticketPage.getTotalElements());
    model.addAttribute("query", query);
    model.addAttribute("selectedStatus", status);
    model.addAttribute("statuses", TicketStatus.values());
    return "tickets/list";
  }

  @GetMapping("/tickets/new")
  public String newTicket(Model model) {
    if (!model.containsAttribute("ticketCreateForm")) {
      model.addAttribute("ticketCreateForm", TicketCreateForm.empty());
    }
    model.addAttribute("priorities", TicketPriority.values());
    return "tickets/create";
  }

  @PostMapping("/tickets")
  public String createTicket(
      @Valid @ModelAttribute("ticketCreateForm") TicketCreateForm form,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("priorities", TicketPriority.values());
      return "tickets/create";
    }

    var ticket = ticketService.create(viewActorId, form.toCommand());
    redirectAttributes.addFlashAttribute("successMessage", "Ticket created successfully");
    return "redirect:/tickets/" + ticket.getId();
  }

  @GetMapping("/tickets/{id}")
  public String ticketDetail(@PathVariable long id, Model model) {
    populateTicketDetail(id, model);
    return "tickets/detail";
  }

  @PostMapping("/tickets/{id}/details")
  public String updateTicketDetails(
      @PathVariable long id,
      @Valid @ModelAttribute("ticketUpdateForm") TicketUpdateForm form,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      populateTicketDetail(id, model);
      return "tickets/detail";
    }
    try {
      ticketService.patch(id, form.toPatch());
      redirectAttributes.addFlashAttribute("successMessage", "Ticket details updated");
    } catch (IllegalArgumentException | UnknownAssigneeException exception) {
      redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/tickets/" + id;
  }

  @PostMapping("/tickets/{id}/status")
  public String transitionTicket(
      @PathVariable long id,
      @RequestParam TicketStatus status,
      RedirectAttributes redirectAttributes) {
    try {
      ticketService.patch(
          id, new TicketPatch(null, null, null, null, false, null, false, status));
      redirectAttributes.addFlashAttribute(
          "successMessage", "Ticket status changed to " + status);
    } catch (InvalidStateTransitionException exception) {
      redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/tickets/" + id;
  }

  @PostMapping("/tickets/{id}/comments")
  public String addComment(
      @PathVariable long id,
      @Valid @ModelAttribute("commentCreateForm") CommentCreateForm form,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      populateTicketDetail(id, model);
      return "tickets/detail";
    }
    commentService.add(viewActorId, id, form.body());
    redirectAttributes.addFlashAttribute("successMessage", "Comment added");
    return "redirect:/tickets/" + id;
  }

  private void populateTicketDetail(long id, Model model) {
    var detail = ticketService.get(id);
    var ticket = detail.ticket();
    model.addAttribute("ticket", ticket);
    model.addAttribute("comments", detail.comments());
    model.addAttribute("allowedStatuses", ticket.getStatus().allowedTargets());
    model.addAttribute("priorities", TicketPriority.values());
    if (!model.containsAttribute("ticketUpdateForm")) {
      model.addAttribute(
          "ticketUpdateForm", new TicketUpdateForm(ticket.getPriority(), ticket.getAssigneeId()));
    }
    if (!model.containsAttribute("commentCreateForm")) {
      model.addAttribute("commentCreateForm", CommentCreateForm.empty());
    }
  }
}
