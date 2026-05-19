package com.sigae.api.model.dto;

import com.sigae.api.model.entity.InvitationStatus;
import java.time.Instant;

public record UserInvitationInfo(
    InvitationStatus status,
    Instant expiresAt
) {}
