package com.sigae.api.service;

import com.sigae.api.exception.ConflictException;
import com.sigae.api.exception.BadRequestException;
import com.sigae.api.exception.NotFoundException;
import com.sigae.api.model.dto.CreateUserRequest;
import com.sigae.api.model.dto.UserInvitationInfo;
import com.sigae.api.model.dto.UpdateUserRequest;
import com.sigae.api.model.dto.UpdateUserStatusRequest;
import com.sigae.api.model.entity.Location;
import com.sigae.api.model.entity.User;
import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import com.sigae.api.repository.LocationRepository;
import com.sigae.api.repository.UserRepository;
import com.sigae.api.security.AuthenticatedUser;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;
  private final LocationRepository locationRepository;
  private final PasswordEncoder passwordEncoder;
  private final PasswordSetupTokenService passwordSetupTokenService;
  private final UserInvitationMailService userInvitationMailService;

  public UserService(
      UserRepository userRepository,
      LocationRepository locationRepository,
      PasswordEncoder passwordEncoder,
      PasswordSetupTokenService passwordSetupTokenService,
      UserInvitationMailService userInvitationMailService
  ) {
    this.userRepository = userRepository;
    this.locationRepository = locationRepository;
    this.passwordEncoder = passwordEncoder;
    this.passwordSetupTokenService = passwordSetupTokenService;
    this.userInvitationMailService = userInvitationMailService;
  }

  @Transactional
  public User create(CreateUserRequest request) {
    String normalizedEmail = normalizeEmail(request.email());
    ensureEmailAvailable(normalizedEmail, null);
    String passwordHash = passwordEncoder.encode(resolveInitialPassword(request));
    Set<Location> assignedLocations = resolveAssignedLocations(request.role(), request.locationIds());

    User user = new User(
        request.fullName().trim(),
        normalizedEmail,
        passwordHash,
        request.role(),
        resolveInitialStatus(request)
    );
    user.setLocations(assignedLocations);
    User createdUser = userRepository.save(user);

    if (request.shouldSendInvitation()) {
      String rawToken = passwordSetupTokenService.issueAccountSetupToken(createdUser);
      userInvitationMailService.sendInvitationMail(createdUser, rawToken);
    }

    return createdUser;
  }

  @Transactional
  public User update(UUID userId, UpdateUserRequest request, AuthenticatedUser authenticatedUser) {
    User user = getById(userId);
    String normalizedEmail = normalizeEmail(request.email());
    ensureEmailAvailable(normalizedEmail, user.getId());
    Set<Location> assignedLocations = resolveAssignedLocations(request.role(), request.locationIds());
    validateRoleChange(user, request.role(), authenticatedUser);

    user.setFullName(request.fullName().trim());
    user.setEmail(normalizedEmail);
    user.setRole(request.role());
    user.setLocations(assignedLocations);
    return userRepository.save(user);
  }

  @Transactional
  public User updateStatus(UUID userId, UpdateUserStatusRequest request, AuthenticatedUser authenticatedUser) {
    User user = getById(userId);
    validateStatusChange(user, request.status(), authenticatedUser);
    user.setStatus(request.status());
    return userRepository.save(user);
  }

  @Transactional
  public User cancelInvitation(UUID userId) {
    User user = getById(userId);
    ensurePendingUserForInvitationAction(user);
    passwordSetupTokenService.cancelInvitation(user.getId());
    return user;
  }

  @Transactional
  public User resendInvitation(UUID userId) {
    User user = getById(userId);
    ensurePendingUserForInvitationAction(user);
    if (passwordSetupTokenService.getInvitationInfo(user.getId()) == null) {
      throw new BadRequestException("El usuario no tiene una invitación previa para reenviar.");
    }
    String rawToken = passwordSetupTokenService.issueAccountSetupToken(user);
    userInvitationMailService.sendInvitationMail(user, rawToken);
    return user;
  }

  @Transactional
  public void markLoginSuccess(User user) {
    user.setLastAccessAt(Instant.now());
    userRepository.save(user);
  }

  @Transactional
  public User createDevAdminIfMissing(String fullName, String email, String rawPassword) {
    return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
        .orElseGet(() -> userRepository.save(new User(
            fullName,
            normalizeEmail(email),
            passwordEncoder.encode(rawPassword),
            com.sigae.api.model.entity.UserRole.ADMINISTRADOR,
            UserStatus.ACTIVE
        )));
  }

  public List<User> findAll() {
    return userRepository.findAll();
  }

  public User getById(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("Usuario no encontrado."));
  }

  public User getByEmailOrThrow(String email) {
    return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
        .orElseThrow(() -> new NotFoundException("Usuario no encontrado."));
  }

  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmailIgnoreCase(normalizeEmail(email));
  }

  public User findActiveByEmail(String email) {
    return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
        .filter(user -> user.getStatus() == UserStatus.ACTIVE)
        .orElseThrow(() -> new NotFoundException("Usuario no encontrado."));
  }

  public UserInvitationInfo getInvitationInfo(User user) {
    if (user.getStatus() != UserStatus.PENDING) {
      return null;
    }

    return passwordSetupTokenService.getInvitationInfo(user.getId());
  }

  private void ensureEmailAvailable(String normalizedEmail, UUID currentUserId) {
    userRepository.findByEmailIgnoreCase(normalizedEmail)
        .filter(existingUser -> !existingUser.getId().equals(currentUserId))
        .ifPresent(existingUser -> {
          throw new ConflictException("Ya existe un usuario con ese correo.");
        });
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private String resolveInitialPassword(CreateUserRequest request) {
    if (request.shouldSendInvitation()) {
      return "SIGAE-" + java.util.UUID.randomUUID();
    }

    if (request.password() == null || request.password().isBlank()) {
      throw new BadRequestException("La contraseña es obligatoria cuando no se envía invitación.");
    }

    return request.password();
  }

  private UserStatus resolveInitialStatus(CreateUserRequest request) {
    return request.shouldSendInvitation() ? UserStatus.PENDING : UserStatus.ACTIVE;
  }

  private void validateRoleChange(User user, UserRole nextRole, AuthenticatedUser authenticatedUser) {
    if (user.getRole() != UserRole.ADMINISTRADOR || nextRole == UserRole.ADMINISTRADOR) {
      return;
    }

    ensureNotSelfAdminMutation(user, authenticatedUser, "No puede quitarse a sí mismo el rol de administrador.");
    ensureNotLastActiveAdministrator(user, "No se puede quitar el rol al último administrador activo.");
  }

  private void validateStatusChange(User user, UserStatus nextStatus, AuthenticatedUser authenticatedUser) {
    if (user.getStatus() == UserStatus.PENDING || nextStatus == UserStatus.PENDING) {
      throw new BadRequestException("Los usuarios pendientes no pueden activarse o desactivarse desde esta sección.");
    }

    if (user.getStatus() == nextStatus) {
      return;
    }

    if (nextStatus == UserStatus.INACTIVE) {
      ensureNotSelfAdminMutation(user, authenticatedUser, "No puede desactivarse a sí mismo.");
      ensureNotLastActiveAdministrator(user, "No se puede desactivar al último administrador activo.");
    }
  }

  private void ensurePendingUserForInvitationAction(User user) {
    if (user.getStatus() != UserStatus.PENDING) {
      throw new BadRequestException("Solo los usuarios pendientes admiten acciones de invitación.");
    }
  }

  private void ensureNotSelfAdminMutation(User targetUser, AuthenticatedUser authenticatedUser, String message) {
    if (authenticatedUser != null && targetUser.getId().equals(authenticatedUser.userId())) {
      throw new ConflictException(message);
    }
  }

  private void ensureNotLastActiveAdministrator(User targetUser, String message) {
    if (targetUser.getRole() != UserRole.ADMINISTRADOR || targetUser.getStatus() != UserStatus.ACTIVE) {
      return;
    }

    long activeAdministrators = userRepository.countByRoleAndStatus(UserRole.ADMINISTRADOR, UserStatus.ACTIVE);
    if (activeAdministrators <= 1) {
      throw new ConflictException(message);
    }
  }

  private Set<Location> resolveAssignedLocations(UserRole role, List<UUID> locationIds) {
    if (role == UserRole.ADMINISTRADOR) {
      return Set.of();
    }

    if (locationIds == null || locationIds.isEmpty()) {
      throw new BadRequestException("Debe asignar al menos una ubicación para este rol.");
    }

    LinkedHashSet<UUID> uniqueLocationIds = new LinkedHashSet<>(locationIds);
    if (uniqueLocationIds.size() != locationIds.size()) {
      throw new BadRequestException("No se pueden repetir ubicaciones asignadas.");
    }

    Map<UUID, Location> locationsById = locationRepository.findAllById(uniqueLocationIds).stream()
        .collect(Collectors.toMap(Location::getId, location -> location));

    if (locationsById.size() != uniqueLocationIds.size()) {
      throw new BadRequestException("Una o más ubicaciones asignadas no existen.");
    }

    return uniqueLocationIds.stream()
        .map(requireLocation(locationsById))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private java.util.function.Function<UUID, Location> requireLocation(Map<UUID, Location> locationsById) {
    return locationId -> {
      Location location = locationsById.get(locationId);
      if (location == null) {
        throw new BadRequestException("Una o más ubicaciones asignadas no existen.");
      }
      return location;
    };
  }
}
