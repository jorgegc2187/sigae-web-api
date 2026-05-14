package com.sigae.api.service;

import com.sigae.api.config.DevSeedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DevInventorySeedBootstrap {

  private static final Logger log = LoggerFactory.getLogger(DevInventorySeedBootstrap.class);

  @Bean
  ApplicationRunner devInventorySeedRunner(
      DevSeedProperties devSeedProperties,
      DevInventorySeedService devInventorySeedService
  ) {
    return args -> {
      if (!devSeedProperties.enabled()) {
        return;
      }

      log.info("dev_inventory_seed_start");
      devInventorySeedService.seed();
      log.info("dev_inventory_seed_complete");
    };
  }
}
