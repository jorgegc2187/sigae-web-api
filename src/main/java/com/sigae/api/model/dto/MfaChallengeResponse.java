package com.sigae.api.model.dto;

public record MfaChallengeResponse(
    String type,
    String challengeToken,
    long expiresIn
) {}
