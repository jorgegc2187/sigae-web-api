package com.sigae.api.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateLoanPayload(
    @NotNull UUID teacherId,
    @NotNull UUID destinationLocationId,
    @NotNull LocalDate loanDate,
    @NotNull LocalDate dueDate,
    String notes,
    @NotEmpty List<UUID> assetIds,
    List<String> attachmentSources
) {}
