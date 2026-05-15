package com.sigae.api.model.dto;

import java.util.UUID;

public record LoanAssetResponse(
    UUID id,
    String code,
    String name,
    String category,
    String status
) {}
