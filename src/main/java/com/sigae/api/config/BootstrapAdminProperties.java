package com.sigae.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap.admin")
public record BootstrapAdminProperties(
    boolean enabled,
    String fullName,
    String email,
    String password
) {}
