package com.sigae.api.service;

import com.sigae.api.config.BootstrapAdminProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DevAdminBootstrap {

  private static final Logger log = LoggerFactory.getLogger(DevAdminBootstrap.class);

  @Bean
  ApplicationRunner bootstrapAdminRunner(
      BootstrapAdminProperties bootstrapAdminProperties,
      UserService userService
  ) {
    return args -> {
      if (!bootstrapAdminProperties.enabled()) {
        return;
      }

      userService.createDevAdminIfMissing(
          bootstrapAdminProperties.fullName(),
          bootstrapAdminProperties.email(),
          bootstrapAdminProperties.password()
      );
      log.info("bootstrap_admin_ready email={}", bootstrapAdminProperties.email());
    };
  }
}
