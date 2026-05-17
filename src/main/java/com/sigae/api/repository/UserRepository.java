package com.sigae.api.repository;

import com.sigae.api.model.entity.User;
import com.sigae.api.model.entity.UserRole;
import com.sigae.api.model.entity.UserStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

  @Override
  @EntityGraph(attributePaths = "locations")
  java.util.List<User> findAll();

  @Override
  @EntityGraph(attributePaths = "locations")
  Optional<User> findById(UUID id);

  @EntityGraph(attributePaths = "locations")
  Optional<User> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);

  long countByRoleAndStatus(UserRole role, UserStatus status);
}
