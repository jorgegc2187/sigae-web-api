package com.sigae.api.model.dto;

public record AuthResponse(
    String type,
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    AuthUserResponse user
) {

  public AuthResponse(
      String accessToken,
      String refreshToken,
      String tokenType,
      long expiresIn,
      AuthUserResponse user
  ) {
    this("AUTHENTICATED", accessToken, refreshToken, tokenType, expiresIn, user);
  }
}
