package com.sigae.api.controller;

import com.sigae.api.model.dto.CreateUserRequest;
import com.sigae.api.model.dto.UpdateUserRequest;
import com.sigae.api.model.dto.UpdateUserStatusRequest;
import com.sigae.api.model.dto.UserResponse;
import com.sigae.api.security.AuthenticatedUser;
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

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping
  public List<UserResponse> list() {
    return userService.findAll().stream().map(UserResponse::from).toList();
  }

  @GetMapping("/{userId}")
  public UserResponse getById(@PathVariable UUID userId) {
    return UserResponse.from(userService.getById(userId));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
    return UserResponse.from(userService.create(request));
  }

  @PatchMapping("/{userId}")
  public UserResponse update(
      @PathVariable UUID userId,
      @Valid @RequestBody UpdateUserRequest request,
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser
  ) {
    return UserResponse.from(userService.update(userId, request, authenticatedUser));
  }

  @PatchMapping("/{userId}/status")
  public UserResponse updateStatus(
      @PathVariable UUID userId,
      @Valid @RequestBody UpdateUserStatusRequest request,
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser
  ) {
    return UserResponse.from(userService.updateStatus(userId, request, authenticatedUser));
  }
}
