package com.sigae.api.repository;

import com.sigae.api.model.entity.PasswordResetRequest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, UUID> {}
