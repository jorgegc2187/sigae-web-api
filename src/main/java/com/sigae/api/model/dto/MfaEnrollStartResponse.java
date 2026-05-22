package com.sigae.api.model.dto;

public record MfaEnrollStartResponse(
    String otpauthUri,
    String manualKey,
    long expiresIn
) {}
