package com.c2.stms.controller;

import com.c2.stms.api.ApiError;
import com.c2.stms.domain.ForbiddenException;
import com.c2.stms.domain.InvalidStateTransitionException;
import com.c2.stms.domain.TicketNotFoundException;
import com.c2.stms.domain.UnauthenticatedException;
import com.c2.stms.domain.UnknownAssigneeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException ex, HttpServletRequest req) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(GlobalExceptionHandler::formatField)
            .collect(Collectors.joining("; "));
    if (message.isBlank()) {
      message =
          ex.getBindingResult().getAllErrors().stream()
              .map(error -> error.getDefaultMessage())
              .collect(Collectors.joining("; "));
    }
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, req);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiError> constraint(ConstraintViolationException ex, HttpServletRequest req) {
    String message =
        ex.getConstraintViolations().stream()
            .map(GlobalExceptionHandler::formatViolation)
            .collect(Collectors.joining("; "));
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, req);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  ResponseEntity<ApiError> handlerValidation(
      HandlerMethodValidationException ex, HttpServletRequest req) {
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", req);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
    String message = "Malformed JSON or invalid field value";
    Throwable cause = ex.getMostSpecificCause();
    if (cause != null && cause.getMessage() != null && cause.getMessage().contains(":")) {
      message = cause.getMessage();
    }
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, req);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<ApiError> typeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
    return error(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_ERROR",
        ex.getName() + ": must be a valid value",
        req);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ApiError> illegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), req);
  }

  @ExceptionHandler(UnknownAssigneeException.class)
  ResponseEntity<ApiError> unknownAssignee(UnknownAssigneeException ex, HttpServletRequest req) {
    return error(HttpStatus.BAD_REQUEST, "UNKNOWN_ASSIGNEE", ex.getMessage(), req);
  }

  @ExceptionHandler(UnauthenticatedException.class)
  ResponseEntity<ApiError> unauthenticated(UnauthenticatedException ex, HttpServletRequest req) {
    return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), req);
  }

  @ExceptionHandler(ForbiddenException.class)
  ResponseEntity<ApiError> forbidden(ForbiddenException ex, HttpServletRequest req) {
    return error(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), req);
  }

  @ExceptionHandler(TicketNotFoundException.class)
  ResponseEntity<ApiError> notFound(TicketNotFoundException ex, HttpServletRequest req) {
    return error(HttpStatus.NOT_FOUND, "TICKET_NOT_FOUND", ex.getMessage(), req);
  }

  @ExceptionHandler(InvalidStateTransitionException.class)
  ResponseEntity<ApiError> conflict(InvalidStateTransitionException ex, HttpServletRequest req) {
    return error(HttpStatus.CONFLICT, "ILLEGAL_TICKET_TRANSITION", ex.getMessage(), req);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest req) {
    return error(
        HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", req);
  }

  private static ResponseEntity<ApiError> error(
      HttpStatus status, String code, String message, HttpServletRequest req) {
    ApiError body =
        new ApiError(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            req.getRequestURI(),
            code);
    return ResponseEntity.status(status).body(body);
  }

  private static String formatField(FieldError error) {
    return error.getField() + ": " + error.getDefaultMessage();
  }

  private static String formatViolation(ConstraintViolation<?> violation) {
    String path = violation.getPropertyPath().toString();
    String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
    return field + ": " + violation.getMessage();
  }
}
