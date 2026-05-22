package com.sigae.api.model.dto;

import com.sigae.api.model.entity.User;
import com.sigae.api.model.entity.Location;
import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record AuthUserResponse(
    UUID id,
    String fullName,
    String email,
    UserRole role,
    UserStatus status,
    Instant lastAccessAt,
    List<UUID> locationIds,
    boolean mfaRequired,
    boolean mfaEnabled,
    Instant mfaEnabledAt
) {

  public static AuthUserResponse from(User user) {
    return from(user, UserMfaStatusResponse.disabled());
  }

  public static AuthUserResponse from(User user, UserMfaStatusResponse mfaStatus) {
    List<UUID> orderedLocationIds = user.getLocations().stream()
        .sorted(Comparator.comparing(Location::getName, String.CASE_INSENSITIVE_ORDER))
        .map(Location::getId)
        .toList();

    return new AuthUserResponse(
        user.getId(),
        user.getFullName(),
        user.getEmail(),
        user.getRole(),
        user.getStatus(),
        user.getLastAccessAt(),
        orderedLocationIds,
        mfaStatus.mfaRequired(),
        mfaStatus.mfaEnabled(),
        mfaStatus.mfaEnabledAt()
    );
  }
}
