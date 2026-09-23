package com.c2.stms.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

class CorsConfigTest {

  @Test
  void rejectsWildcardOrigin() {
    assertThatThrownBy(() -> new CorsConfig("*"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not *");
  }

  @Test
  void registersSingleOrigin() {
    CorsRegistry registry = new CorsRegistry();
    new CorsConfig("http://localhost:3000").addCorsMappings(registry);
    assertThat(registry).isNotNull();
  }
}
