package com.sigae.api.controller;

import com.sigae.api.model.dto.AuthResponse;
import com.sigae.api.model.dto.AuthUserResponse;
import com.sigae.api.model.dto.ForgotPasswordRequest;
import com.sigae.api.model.dto.ForgotPasswordResponse;
import com.sigae.api.model.dto.LoginRequest;
import com.sigae.api.model.dto.LogoutRequest;
import com.sigae.api.model.dto.RefreshTokenRequest;
import com.sigae.api.model.dto.ResetPasswordRequest;
import com.sigae.api.service.AuthService;
import com.sigae.api.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request.email(), request.password());
  }

  @PostMapping("/refresh")
  public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return authService.refresh(request.refreshToken());
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@Valid @RequestBody LogoutRequest request) {
    authService.logout(request.refreshToken());
  }

  @GetMapping("/me")
  public AuthUserResponse me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
    return authService.me(authenticatedUser);
  }

  @PostMapping("/forgot-password")
  public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    authService.requestPasswordReset(request);
    return new ForgotPasswordResponse(
        "Si el correo está registrado, recibirás instrucciones de recuperación en los próximos minutos."
    );
  }

  @PostMapping("/reset-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    authService.resetPassword(request);
  }
}
