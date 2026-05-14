package com.sigae.api.controller;

import com.sigae.api.model.dto.TeacherRequest;
import com.sigae.api.model.dto.TeacherResponse;
import com.sigae.api.service.TeacherService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

  private final TeacherService teacherService;

  public TeacherController(TeacherService teacherService) {
    this.teacherService = teacherService;
  }

  @GetMapping
  public List<TeacherResponse> list() {
    return teacherService.findAll().stream().map(TeacherResponse::from).toList();
  }

  @GetMapping("/{teacherId}")
  public TeacherResponse getById(@PathVariable UUID teacherId) {
    return TeacherResponse.from(teacherService.getById(teacherId));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public TeacherResponse create(@Valid @RequestBody TeacherRequest request) {
    return TeacherResponse.from(teacherService.create(request));
  }

  @PatchMapping("/{teacherId}")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public TeacherResponse update(
      @PathVariable UUID teacherId,
      @Valid @RequestBody TeacherRequest request
  ) {
    return TeacherResponse.from(teacherService.update(teacherId, request));
  }
}
