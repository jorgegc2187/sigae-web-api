package com.sigae.api.controller;

import com.sigae.api.model.dto.CreateUserRequest;
import com.sigae.api.model.dto.UpdateUserRequest;
import com.sigae.api.model.dto.UpdateUserMfaPolicyRequest;
import com.sigae.api.model.dto.UpdateUserStatusRequest;
import com.sigae.api.model.dto.UserResponse;
import com.sigae.api.security.AuthenticatedUser;
import com.sigae.api.service.MfaService;
import com.sigae.api.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UserController {

  private final UserService userService;
  private final MfaService mfaService;

  public UserController(UserService userService, MfaService mfaService) {
    this.userService = userService;
    this.mfaService = mfaService;
  }

  @GetMapping
  public List<UserResponse> list() {
    return userService.findAll().stream().map(this::toResponse).toList();
  }

  @GetMapping("/{userId}")
  public UserResponse getById(@PathVariable UUID userId) {
    return toResponse(userService.getById(userId));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
    return toResponse(userService.create(request));
  }

  @PatchMapping("/{userId}")
  public UserResponse update(
      @PathVariable UUID userId,
      @Valid @RequestBody UpdateUserRequest request,
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser
  ) {
    return toResponse(userService.update(userId, request, authenticatedUser));
  }

  @PatchMapping("/{userId}/status")
  public UserResponse updateStatus(
      @PathVariable UUID userId,
      @Valid @RequestBody UpdateUserStatusRequest request,
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser
  ) {
    return toResponse(userService.updateStatus(userId, request, authenticatedUser));
  }

  @PostMapping("/{userId}/invitation/cancel")
  public UserResponse cancelInvitation(@PathVariable UUID userId) {
    return toResponse(userService.cancelInvitation(userId));
  }

  @PostMapping("/{userId}/invitation/resend")
  public UserResponse resendInvitation(@PathVariable UUID userId) {
    return toResponse(userService.resendInvitation(userId));
  }

  @PostMapping("/{userId}/password-reset")
  public UserResponse requestPasswordReset(@PathVariable UUID userId) {
    return toResponse(userService.requestPasswordReset(userId));
  }

  @PatchMapping("/{userId}/mfa-policy")
  public UserResponse updateMfaPolicy(
      @PathVariable UUID userId,
      @Valid @RequestBody UpdateUserMfaPolicyRequest request
  ) {
    mfaService.updatePolicy(userId, request.mfaRequired());
    return toResponse(userService.getById(userId));
  }

  @PostMapping("/{userId}/mfa-reset")
  public UserResponse resetMfa(@PathVariable UUID userId) {
    mfaService.reset(userId);
    return toResponse(userService.getById(userId));
  }

  private UserResponse toResponse(com.sigae.api.model.entity.User user) {
    return UserResponse.from(user, userService.getInvitationInfo(user), mfaService.getStatus(user));
  }
}
