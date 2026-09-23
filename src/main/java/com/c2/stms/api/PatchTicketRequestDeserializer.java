package com.c2.stms.api;

import com.c2.stms.domain.TicketPriority;
import com.c2.stms.domain.TicketStatus;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;
import java.time.Instant;

public class PatchTicketRequestDeserializer extends ValueDeserializer<PatchTicketRequest> {

  @Override
  public PatchTicketRequest deserialize(JsonParser parser, DeserializationContext context)
      throws JacksonException {
    JsonNode node = parser.readValueAsTree();
    if (node == null || node.isNull()) {
      return new PatchTicketRequest(
          null, null, null, null, false, null, false, null, null, null, null);
    }
    return new PatchTicketRequest(
        text(node, "title"),
        text(node, "description"),
        enumValue(parser, node, "priority", TicketPriority.class),
        text(node, "category"),
        node.has("category"),
        longValue(node, "assigneeId"),
        node.has("assigneeId"),
        enumValue(parser, node, "status", TicketStatus.class),
        longValue(node, "id"),
        longValue(node, "reporterId"),
        instantValue(parser, node, "createdAt"));
  }

  private static String text(JsonNode node, String field) {
    if (!node.has(field) || node.get(field).isNull()) {
      return null;
    }
    String trimmed = node.get(field).asString().trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static Long longValue(JsonNode node, String field) {
    if (!node.has(field) || node.get(field).isNull()) {
      return null;
    }
    return node.get(field).asLong();
  }

  private static Instant instantValue(JsonParser parser, JsonNode node, String field)
      throws JacksonException {
    if (!node.has(field) || node.get(field).isNull()) {
      return null;
    }
    try {
      return Instant.parse(node.get(field).asString());
    } catch (RuntimeException ex) {
      throw InvalidFormatException.from(
          parser, field + ": invalid instant", node.get(field), Instant.class);
    }
  }

  private static <E extends Enum<E>> E enumValue(
      JsonParser parser, JsonNode node, String field, Class<E> type) throws JacksonException {
    if (!node.has(field) || node.get(field).isNull()) {
      return null;
    }
    String raw = node.get(field).asString();
    try {
      return Enum.valueOf(type, raw);
    } catch (IllegalArgumentException ex) {
      throw InvalidFormatException.from(parser, field + ": must be a known value", raw, type);
    }
  }
}
