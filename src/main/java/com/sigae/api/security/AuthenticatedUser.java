package com.sigae.api.security;

import com.sigae.api.model.entity.UserRole;
import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(
    UUID userId,
    String email,
    UserRole role,
    List<String> locationIds
) {}
