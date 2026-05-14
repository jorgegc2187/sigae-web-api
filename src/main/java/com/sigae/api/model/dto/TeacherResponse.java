package com.sigae.api.model.dto;

import com.sigae.api.model.entity.CatalogStatus;
import com.sigae.api.model.entity.Teacher;
import java.util.UUID;

public record TeacherResponse(
    UUID id,
    String dni,
    String fullName,
    String specialty,
    String email,
    String phone,
    CatalogStatus status
) {
  public static TeacherResponse from(Teacher teacher) {
    return new TeacherResponse(
        teacher.getId(),
        teacher.getDni(),
        teacher.getFullName(),
        teacher.getSpecialty(),
        teacher.getEmail(),
        teacher.getPhone(),
        teacher.getStatus()
    );
  }
}
