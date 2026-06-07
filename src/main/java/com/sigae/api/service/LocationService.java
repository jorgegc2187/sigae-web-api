package com.sigae.api.service;

import com.sigae.api.exception.ConflictException;
import com.sigae.api.exception.BadRequestException;
import com.sigae.api.exception.NotFoundException;
import com.sigae.api.model.dto.LocationRequest;
import com.sigae.api.model.dto.LocationManagerResponse;
import com.sigae.api.model.dto.LocationResponse;
import com.sigae.api.model.dto.UpdateLocationStatusRequest;
import com.sigae.api.model.entity.CatalogStatus;
import com.sigae.api.model.entity.Location;
import com.sigae.api.model.entity.User;
import com.sigae.api.model.entity.UserRole;
import com.sigae.api.repository.LocationRepository;
import com.sigae.api.repository.UserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LocationService {

  private final LocationRepository locationRepository;
  private final UserRepository userRepository;

  public LocationService(LocationRepository locationRepository, UserRepository userRepository) {
    this.locationRepository = locationRepository;
    this.userRepository = userRepository;
  }

  public List<LocationResponse> findAll(CatalogStatus status) {
    List<Location> locations = status == null
        ? locationRepository.findAll()
        : locationRepository.findAllByStatus(status);
    return toResponses(locations);
  }

  public LocationResponse getResponseById(UUID id) {
    return toResponse(getEntityById(id));
  }

  public Location getEntityById(UUID id) {
    return locationRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Ubicación no encontrada."));
  }

  @Transactional
  public LocationResponse create(LocationRequest request) {
    ensureNameAvailable(request.name(), null);
    Location location = locationRepository.save(new Location(
        request.name().trim(),
        request.description().trim(),
        request.status()
    ));
    syncManagers(location, request.managerIds());
    return toResponse(location);
  }

  @Transactional
  public LocationResponse update(UUID id, LocationRequest request) {
    Location location = getEntityById(id);
    ensureNameAvailable(request.name(), location.getId());
    location.setName(request.name().trim());
    location.setDescription(request.description().trim());
    location.setStatus(request.status());
    Location updatedLocation = locationRepository.save(location);
    syncManagers(updatedLocation, request.managerIds());
    return toResponse(updatedLocation);
  }

  @Transactional
  public LocationResponse updateStatus(UUID id, UpdateLocationStatusRequest request) {
    Location location = getEntityById(id);
    location.setStatus(request.status());
    return toResponse(locationRepository.save(location));
  }

  private void ensureNameAvailable(String name, UUID currentId) {
    locationRepository.findByNameIgnoreCase(name.trim())
        .filter(location -> !location.getId().equals(currentId))
        .ifPresent(location -> {
          throw new ConflictException("Ya existe una ubicación con ese nombre.");
        });
  }

  private List<LocationResponse> toResponses(List<Location> locations) {
    if (locations.isEmpty()) {
      return List.of();
    }

    Map<UUID, List<LocationManagerResponse>> managersByLocation = resolveManagersByLocation(locations);
    return locations.stream()
        .sorted(Comparator.comparing(Location::getName, String.CASE_INSENSITIVE_ORDER))
        .map(location -> LocationResponse.from(location, managersByLocation.getOrDefault(location.getId(), List.of())))
        .toList();
  }

  private LocationResponse toResponse(Location location) {
    return toResponses(List.of(location)).getFirst();
  }

  private Map<UUID, List<LocationManagerResponse>> resolveManagersByLocation(List<Location> locations) {
    Set<UUID> locationIds = locations.stream().map(Location::getId).collect(Collectors.toSet());
    Map<UUID, List<LocationManagerResponse>> managersByLocation = new LinkedHashMap<>();

    for (User manager : userRepository.findAllByRole(UserRole.ENCARGADO)) {
      LocationManagerResponse managerResponse = LocationManagerResponse.from(manager);
      for (Location location : manager.getLocations()) {
        if (!locationIds.contains(location.getId())) {
          continue;
        }
        managersByLocation.computeIfAbsent(location.getId(), ignored -> new ArrayList<>()).add(managerResponse);
      }
    }

    managersByLocation.values().forEach(managers ->
        managers.sort(Comparator.comparing(LocationManagerResponse::fullName, String.CASE_INSENSITIVE_ORDER)));

    return managersByLocation;
  }

  private void syncManagers(Location location, List<UUID> managerIds) {
    LinkedHashSet<UUID> uniqueManagerIds = normalizeManagerIds(managerIds);
    List<User> currentManagers = userRepository.findAllByRole(UserRole.ENCARGADO).stream()
        .filter(user -> user.getLocations().stream().anyMatch(assignedLocation -> assignedLocation.getId().equals(location.getId())))
        .toList();

    Map<UUID, User> selectedManagersById = resolveSelectedManagers(uniqueManagerIds);
    List<User> usersToSave = new ArrayList<>();

    for (User manager : currentManagers) {
      if (selectedManagersById.containsKey(manager.getId())) {
        continue;
      }
      Set<Location> nextLocations = new LinkedHashSet<>(manager.getLocations());
      nextLocations.removeIf(assignedLocation -> assignedLocation.getId().equals(location.getId()));
      manager.setLocations(nextLocations);
      usersToSave.add(manager);
    }

    for (User manager : selectedManagersById.values()) {
      if (manager.getLocations().stream().anyMatch(assignedLocation -> assignedLocation.getId().equals(location.getId()))) {
        continue;
      }
      Set<Location> nextLocations = new LinkedHashSet<>(manager.getLocations());
      nextLocations.add(location);
      manager.setLocations(nextLocations);
      usersToSave.add(manager);
    }

    if (!usersToSave.isEmpty()) {
      userRepository.saveAll(usersToSave);
    }
  }

  private LinkedHashSet<UUID> normalizeManagerIds(List<UUID> managerIds) {
    if (managerIds == null || managerIds.isEmpty()) {
      return new LinkedHashSet<>();
    }

    LinkedHashSet<UUID> uniqueManagerIds = new LinkedHashSet<>(managerIds);
    if (uniqueManagerIds.size() != managerIds.size()) {
      throw new BadRequestException("No se pueden repetir encargados en una ubicación.");
    }
    return uniqueManagerIds;
  }

  private Map<UUID, User> resolveSelectedManagers(LinkedHashSet<UUID> managerIds) {
    if (managerIds.isEmpty()) {
      return Map.of();
    }

    Map<UUID, User> usersById = userRepository.findAllByIdIn(managerIds).stream()
        .collect(Collectors.toMap(User::getId, Function.identity()));

    if (usersById.size() != managerIds.size()) {
      throw new BadRequestException("Uno o más encargados seleccionados no existen.");
    }

    for (User user : usersById.values()) {
      if (user.getRole() != UserRole.ENCARGADO) {
        throw new BadRequestException("Solo los usuarios con rol Encargado pueden asignarse a una ubicación.");
      }
    }

    return managerIds.stream()
        .collect(Collectors.toMap(Function.identity(), usersById::get, (first, second) -> first, LinkedHashMap::new));
  }
}
