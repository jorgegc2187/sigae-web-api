package com.sigae.api.model.dto;

import java.util.UUID;

public record PhysicalInventoryReportRowResponse(
    UUID id,
    String code,
    String assetDescription,
    String location,
    boolean good,
    boolean regular,
    boolean bad,
    String brand,
    String observations,
    int quantity
) {}
