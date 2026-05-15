package com.sigae.api.repository;

import com.sigae.api.model.entity.LoanAttachment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanAttachmentRepository extends JpaRepository<LoanAttachment, UUID> {
  Optional<LoanAttachment> findByIdAndLoanId(UUID id, UUID loanId);
}
