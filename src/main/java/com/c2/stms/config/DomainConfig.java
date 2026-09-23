package com.c2.stms.config;

import com.c2.stms.domain.TicketStateMachine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

  @Bean
  TicketStateMachine ticketStateMachine() {
    return new TicketStateMachine();
  }
}
