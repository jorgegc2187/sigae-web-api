package com.sigae.api.service;

import com.sigae.api.exception.ConflictException;
import com.sigae.api.exception.NotFoundException;
import com.sigae.api.model.dto.TeacherRequest;
import com.sigae.api.model.entity.Teacher;
import com.sigae.api.repository.TeacherRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TeacherService {

  private final TeacherRepository teacherRepository;

  public TeacherService(TeacherRepository teacherRepository) {
    this.teacherRepository = teacherRepository;
  }

  public List<Teacher> findAll() {
    return teacherRepository.findAll();
  }

  public Teacher getById(UUID id) {
    return teacherRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Docente no encontrado."));
  }

  @Transactional
  public Teacher create(TeacherRequest request) {
    ensureDniAvailable(request.dni(), null);
    return teacherRepository.save(new Teacher(
        request.dni().trim(),
        request.fullName().trim(),
        normalizeOptional(request.specialty()),
        normalizeOptional(request.email()),
        normalizeOptional(request.phone()),
        request.status()
    ));
  }

  @Transactional
  public Teacher update(UUID id, TeacherRequest request) {
    Teacher teacher = getById(id);
    ensureDniAvailable(request.dni(), teacher.getId());
    teacher.setDni(request.dni().trim());
    teacher.setFullName(request.fullName().trim());
    teacher.setSpecialty(normalizeOptional(request.specialty()));
    teacher.setEmail(normalizeOptional(request.email()));
    teacher.setPhone(normalizeOptional(request.phone()));
    teacher.setStatus(request.status());
    return teacherRepository.save(teacher);
  }

  private void ensureDniAvailable(String dni, UUID currentId) {
    teacherRepository.findByDni(dni.trim())
        .filter(teacher -> !teacher.getId().equals(currentId))
        .ifPresent(teacher -> {
          throw new ConflictException("Ya existe un docente con ese DNI.");
        });
  }

  private String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
