package com.sigae.api.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FlywayConfig {

  @Bean(initMethod = "migrate")
  Flyway flyway(DataSource dataSource) {
    return Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .load();
  }
}
