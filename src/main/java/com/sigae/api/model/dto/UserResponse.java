package com.sigae.api.model.dto;

import com.sigae.api.model.entity.User;
import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String fullName,
    String email,
    UserRole role,
    UserStatus status,
    Instant lastAccessAt
) {

  public static UserResponse from(User user) {
    return new UserResponse(
        user.getId(),
        user.getFullName(),
        user.getEmail(),
        user.getRole(),
        user.getStatus(),
        user.getLastAccessAt()
    );
  }
}
