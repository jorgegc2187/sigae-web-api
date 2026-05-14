package com.sigae.api.repository;

import com.sigae.api.model.entity.Teacher;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, UUID> {
  Optional<Teacher> findByDni(String dni);
}
