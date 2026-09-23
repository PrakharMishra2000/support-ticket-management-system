package com.c2.stms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

  private final String allowedOrigin;

  public CorsConfig(@Value("${stms.cors.allowed-origin}") String allowedOrigin) {
    if (allowedOrigin == null || allowedOrigin.isBlank() || "*".equals(allowedOrigin.trim())) {
      throw new IllegalArgumentException("stms.cors.allowed-origin must be a single origin, not *");
    }
    this.allowedOrigin = allowedOrigin.trim();
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOrigins(allowedOrigin)
        .allowedMethods("GET", "POST", "PATCH", "OPTIONS")
        .allowedHeaders("Content-Type", "X-Actor-Id")
        .allowCredentials(true);
  }
}
