package com.sigae.api.controller;

import com.sigae.api.model.dto.LocationRequest;
import com.sigae.api.model.dto.LocationResponse;
import com.sigae.api.service.LocationService;
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
@RequestMapping("/api/locations")
public class LocationController {

  private final LocationService locationService;

  public LocationController(LocationService locationService) {
    this.locationService = locationService;
  }

  @GetMapping
  public List<LocationResponse> list() {
    return locationService.findAll().stream().map(LocationResponse::from).toList();
  }

  @GetMapping("/{locationId}")
  public LocationResponse getById(@PathVariable UUID locationId) {
    return LocationResponse.from(locationService.getById(locationId));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public LocationResponse create(@Valid @RequestBody LocationRequest request) {
    return LocationResponse.from(locationService.create(request));
  }

  @PatchMapping("/{locationId}")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public LocationResponse update(
      @PathVariable UUID locationId,
      @Valid @RequestBody LocationRequest request
  ) {
    return LocationResponse.from(locationService.update(locationId, request));
  }
}
