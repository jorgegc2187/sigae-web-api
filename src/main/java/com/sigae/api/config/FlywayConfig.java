package com.sigae.api.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

  @Bean(initMethod = "migrate")
  Flyway flyway(DataSource dataSource) {
    return Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .load();
  }

  @Bean
  ApplicationRunner flywayMigrationRunner(Flyway flyway) {
    return args -> flyway.migrate();
  }
}
