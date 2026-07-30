package com.sigae.api.model.dto;

import java.time.LocalDate;
import java.util.List;

public record PhysicalInventoryReportResponse(
    String ugelName,
    String institutionName,
    String title,
    String locationSubtitle,
    String generatedBy,
    LocalDate generatedAt,
    List<PhysicalInventoryReportRowResponse> rows
) {}
