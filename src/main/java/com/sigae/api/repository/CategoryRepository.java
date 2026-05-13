package com.sigae.api.repository;

import com.sigae.api.model.entity.Category;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

  @Override
  @EntityGraph(attributePaths = "types")
  java.util.List<Category> findAll();

  boolean existsByNameIgnoreCase(String name);

  Optional<Category> findByNameIgnoreCase(String name);
}
