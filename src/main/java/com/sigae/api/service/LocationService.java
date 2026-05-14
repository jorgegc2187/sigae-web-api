package com.sigae.api.service;

import com.sigae.api.exception.ConflictException;
import com.sigae.api.exception.NotFoundException;
import com.sigae.api.model.dto.LocationRequest;
import com.sigae.api.model.entity.Location;
import com.sigae.api.repository.LocationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LocationService {

  private final LocationRepository locationRepository;

  public LocationService(LocationRepository locationRepository) {
    this.locationRepository = locationRepository;
  }

  public List<Location> findAll() {
    return locationRepository.findAll();
  }

  public Location getById(UUID id) {
    return locationRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Ubicación no encontrada."));
  }

  @Transactional
  public Location create(LocationRequest request) {
    ensureNameAvailable(request.name(), null);
    return locationRepository.save(new Location(
        request.name().trim(),
        request.description().trim(),
        request.status()
    ));
  }

  @Transactional
  public Location update(UUID id, LocationRequest request) {
    Location location = getById(id);
    ensureNameAvailable(request.name(), location.getId());
    location.setName(request.name().trim());
    location.setDescription(request.description().trim());
    location.setStatus(request.status());
    return locationRepository.save(location);
  }

  private void ensureNameAvailable(String name, UUID currentId) {
    locationRepository.findByNameIgnoreCase(name.trim())
        .filter(location -> !location.getId().equals(currentId))
        .ifPresent(location -> {
          throw new ConflictException("Ya existe una ubicación con ese nombre.");
        });
  }
}
