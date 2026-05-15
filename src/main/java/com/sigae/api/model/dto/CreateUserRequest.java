package com.sigae.api.model.dto;

import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank @Size(max = 160) String fullName,
    @NotBlank @Email @Size(max = 320) String email,
    @Size(min = 8, max = 120) String password,
    @NotNull UserRole role,
    @NotNull UserStatus status,
    Boolean sendInvitation
) {
  public boolean shouldSendInvitation() {
    return Boolean.TRUE.equals(sendInvitation);
  }
}
