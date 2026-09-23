package com.c2.stms;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StmsApplicationTests {

  @Autowired private DataSource dataSource;

  @Test
  void contextLoads() {}

  @Test
  void hibernateCreatesTicketsAndCommentsWithoutCommentUpdatedAt() throws Exception {
    try (Connection connection = dataSource.getConnection();
        var tables = connection.getMetaData().getTables(null, null, "%", new String[] {"TABLE"})) {
      Set<String> names = new HashSet<>();
      while (tables.next()) {
        names.add(tables.getString("TABLE_NAME").toLowerCase());
      }
      assertThat(names).contains("tickets", "comments").doesNotContain("flyway_schema_history");
    }

    try (Connection connection = dataSource.getConnection();
        var columns = connection.getMetaData().getColumns(null, null, "COMMENTS", "%")) {
      Set<String> commentsColumns = new HashSet<>();
      while (columns.next()) {
        commentsColumns.add(columns.getString("COLUMN_NAME").toLowerCase());
      }
      assertThat(commentsColumns)
          .contains("id", "ticket_id", "body", "author_id", "created_at")
          .doesNotContain("updated_at");
    }
  }
}
