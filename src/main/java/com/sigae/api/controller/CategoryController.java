package com.sigae.api.controller;

import com.sigae.api.model.dto.AssetTypeResponse;
import com.sigae.api.model.dto.CategoryResponse;
import com.sigae.api.model.dto.CreateAssetTypeRequest;
import com.sigae.api.model.dto.CreateCategoryRequest;
import com.sigae.api.model.dto.UpdateAssetTypeRequest;
import com.sigae.api.model.dto.UpdateCategoryRequest;
import com.sigae.api.service.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

  private final CategoryService categoryService;

  public CategoryController(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  @GetMapping
  public List<CategoryResponse> list() {
    return categoryService.findAll().stream().map(CategoryResponse::from).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public CategoryResponse create(@Valid @RequestBody CreateCategoryRequest request) {
    return CategoryResponse.from(categoryService.createCategory(request));
  }

  @PatchMapping("/{categoryId}")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public CategoryResponse update(
      @PathVariable UUID categoryId,
      @Valid @RequestBody UpdateCategoryRequest request
  ) {
    return CategoryResponse.from(categoryService.updateCategory(categoryId, request));
  }

  @DeleteMapping("/{categoryId}")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID categoryId) {
    categoryService.deleteCategory(categoryId);
  }

  @PostMapping("/{categoryId}/types")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public AssetTypeResponse createType(
      @PathVariable UUID categoryId,
      @Valid @RequestBody CreateAssetTypeRequest request
  ) {
    return AssetTypeResponse.from(categoryService.createAssetType(categoryId, request));
  }

  @PatchMapping("/{categoryId}/types/{typeId}")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public AssetTypeResponse updateType(
      @PathVariable UUID categoryId,
      @PathVariable UUID typeId,
      @Valid @RequestBody UpdateAssetTypeRequest request
  ) {
    return AssetTypeResponse.from(categoryService.updateAssetType(categoryId, typeId, request));
  }

  @DeleteMapping("/{categoryId}/types/{typeId}")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteType(@PathVariable UUID categoryId, @PathVariable UUID typeId) {
    categoryService.deleteAssetType(categoryId, typeId);
  }
}
