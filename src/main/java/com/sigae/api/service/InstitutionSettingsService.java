package com.sigae.api.service;

import com.sigae.api.exception.BadRequestException;
import com.sigae.api.model.dto.InstitutionLogoFile;
import com.sigae.api.model.dto.UpdateInstitutionSettingsRequest;
import com.sigae.api.model.entity.InstitutionSettings;
import com.sigae.api.repository.InstitutionSettingsRepository;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class InstitutionSettingsService {

  private static final long MAX_LOGO_SIZE_BYTES = 2 * 1024 * 1024;

  private final InstitutionSettingsRepository institutionSettingsRepository;

  public InstitutionSettingsService(InstitutionSettingsRepository institutionSettingsRepository) {
    this.institutionSettingsRepository = institutionSettingsRepository;
  }

  @Transactional
  public InstitutionSettings getCurrentSettings() {
    return institutionSettingsRepository.findById(InstitutionSettings.SINGLETON_ID)
        .orElseGet(this::createDefaultSettings);
  }

  public Optional<InstitutionLogoFile> getLogoFile() {
    InstitutionSettings settings = getCurrentSettings();
    if (!settings.hasLogo()) {
      return Optional.empty();
    }

    return Optional.of(new InstitutionLogoFile(
        settings.getLogoFileName() == null ? "institution-logo" : settings.getLogoFileName(),
        MediaType.parseMediaType(settings.getLogoMimeType()),
        settings.getLogoContent()
    ));
  }

  @Transactional
  public InstitutionSettings update(UpdateInstitutionSettingsRequest request, MultipartFile logo) {
    InstitutionSettings settings = getCurrentSettings();
    settings.update(
        request.systemName().trim(),
        normalizeOptional(request.address()),
        normalizeOptional(request.city()),
        normalizeOptional(request.supportPhone()),
        request.supportEmail().trim().toLowerCase(Locale.ROOT)
    );

    if (logo != null && !logo.isEmpty()) {
      validateLogo(logo);
      try {
        settings.updateLogo(
            normalizeFilename(logo.getOriginalFilename(), "institution-logo"),
            logo.getContentType(),
            logo.getBytes()
        );
      } catch (IOException exception) {
        throw new BadRequestException("No se pudo procesar el logo institucional.");
      }
    }

    return institutionSettingsRepository.save(settings);
  }

  private void validateLogo(MultipartFile logo) {
    String contentType = normalizeOptional(logo.getContentType());
    if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
      throw new BadRequestException("El logo institucional debe ser una imagen válida.");
    }

    if (logo.getSize() > MAX_LOGO_SIZE_BYTES) {
      throw new BadRequestException("El logo institucional no puede superar los 2 MB.");
    }
  }

  private InstitutionSettings createDefaultSettings() {
    InstitutionSettings settings = new InstitutionSettings(
        "SIGAE",
        null,
        null,
        null,
        "contacto@institucion.edu.pe"
    );
    return institutionSettingsRepository.save(settings);
  }

  private String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String normalizeFilename(String value, String fallback) {
    String normalized = normalizeOptional(value);
    return normalized == null ? fallback : normalized;
  }
}
