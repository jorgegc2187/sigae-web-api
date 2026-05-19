package com.sigae.api.model.dto;

import com.sigae.api.model.entity.User;
import com.sigae.api.model.entity.InvitationStatus;
import com.sigae.api.model.entity.Location;
import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String fullName,
    String email,
    UserRole role,
    UserStatus status,
    Instant lastAccessAt,
    List<UUID> locationIds,
    List<String> locationNames,
    InvitationStatus invitationStatus,
    Instant invitationExpiresAt
) {

  public static UserResponse from(User user) {
    return from(user, null);
  }

  public static UserResponse from(User user, UserInvitationInfo invitationInfo) {
    List<Location> orderedLocations = user.getLocations().stream()
        .sorted(Comparator.comparing(Location::getName, String.CASE_INSENSITIVE_ORDER))
        .toList();

    return new UserResponse(
        user.getId(),
        user.getFullName(),
        user.getEmail(),
        user.getRole(),
        user.getStatus(),
        user.getLastAccessAt(),
        orderedLocations.stream().map(Location::getId).toList(),
        orderedLocations.stream().map(Location::getName).toList(),
        invitationInfo != null ? invitationInfo.status() : null,
        invitationInfo != null ? invitationInfo.expiresAt() : null
    );
  }
}
