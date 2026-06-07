package com.sigae.api.model.dto;

import com.sigae.api.model.entity.User;
import java.util.UUID;

public record LocationManagerResponse(
    UUID id,
    String fullName
) {
  public static LocationManagerResponse from(User user) {
    return new LocationManagerResponse(user.getId(), user.getFullName());
  }
}
