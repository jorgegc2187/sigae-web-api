package com.sigae.api.service;

import com.sigae.api.model.dto.AttributeDefinitionRequest;
import com.sigae.api.model.dto.CreateAssetTypeRequest;
import com.sigae.api.model.dto.CreateCategoryRequest;
import com.sigae.api.model.dto.UpdateAssetTypeRequest;
import com.sigae.api.model.dto.UpdateCategoryRequest;
import com.sigae.api.model.entity.AssetAttributeDefinition;
import com.sigae.api.model.entity.AssetType;
import com.sigae.api.model.entity.Category;
import com.sigae.api.repository.AssetTypeRepository;
import com.sigae.api.repository.CategoryRepository;
import com.sigae.api.exception.ConflictException;
import com.sigae.api.exception.NotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {

  private final CategoryRepository categoryRepository;
  private final AssetTypeRepository assetTypeRepository;

  public CategoryService(CategoryRepository categoryRepository, AssetTypeRepository assetTypeRepository) {
    this.categoryRepository = categoryRepository;
    this.assetTypeRepository = assetTypeRepository;
  }

  public List<Category> findAll() {
    return categoryRepository.findAll();
  }

  @Transactional
  public Category createCategory(CreateCategoryRequest request) {
    ensureCategoryNameAvailable(request.name(), null);
    Category category = new Category(request.name().trim(), request.icon().trim());
    return categoryRepository.save(category);
  }

  @Transactional
  public Category updateCategory(UUID categoryId, UpdateCategoryRequest request) {
    Category category = getCategoryById(categoryId);
    ensureCategoryNameAvailable(request.name(), category.getId());
    category.setName(request.name().trim());
    category.setIcon(request.icon().trim());
    return categoryRepository.save(category);
  }

  @Transactional
  public void deleteCategory(UUID categoryId) {
    categoryRepository.delete(getCategoryById(categoryId));
  }

  @Transactional
  public AssetType createAssetType(UUID categoryId, CreateAssetTypeRequest request) {
    Category category = getCategoryById(categoryId);
    ensureTypeNameAvailable(categoryId, request.name(), null);
    AssetType assetType = new AssetType(request.name().trim(), request.icon().trim());
    assetType.replaceAttributes(request.attributes().stream().map(this::toAttributeEntity).toList());
    category.addType(assetType);
    categoryRepository.save(category);
    return assetType;
  }

  @Transactional
  public AssetType updateAssetType(UUID categoryId, UUID typeId, UpdateAssetTypeRequest request) {
    AssetType assetType = getAssetTypeById(typeId);
    if (!assetType.getCategory().getId().equals(categoryId)) {
      throw new NotFoundException("Tipo de activo no encontrado para la categoría indicada.");
    }

    Category targetCategory = getCategoryById(request.categoryId());
    ensureTypeNameAvailable(targetCategory.getId(), request.name(), assetType.getId());

    assetType.setName(request.name().trim());
    assetType.setIcon(request.icon().trim());
    assetType.setCategory(targetCategory);
    assetType.replaceAttributes(mergeAttributes(assetType, request.attributes()));
    return assetTypeRepository.save(assetType);
  }

  @Transactional
  public void deleteAssetType(UUID categoryId, UUID typeId) {
    AssetType assetType = getAssetTypeById(typeId);
    if (!assetType.getCategory().getId().equals(categoryId)) {
      throw new NotFoundException("Tipo de activo no encontrado para la categoría indicada.");
    }
    assetTypeRepository.delete(assetType);
  }

  private Category getCategoryById(UUID categoryId) {
    return categoryRepository.findById(categoryId)
        .orElseThrow(() -> new NotFoundException("Categoría no encontrada."));
  }

  private AssetType getAssetTypeById(UUID typeId) {
    return assetTypeRepository.findWithCategoryAndAttributesById(typeId)
        .orElseThrow(() -> new NotFoundException("Tipo de activo no encontrado."));
  }

  private void ensureCategoryNameAvailable(String name, UUID currentId) {
    String normalizedName = normalizeName(name);
    categoryRepository.findByNameIgnoreCase(normalizedName)
        .filter(category -> !category.getId().equals(currentId))
        .ifPresent(category -> {
          throw new ConflictException("Ya existe una categoría con ese nombre.");
        });
  }

  private void ensureTypeNameAvailable(UUID categoryId, String name, UUID currentId) {
    String normalizedName = normalizeName(name);
    if (currentId == null && assetTypeRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, normalizedName)) {
      throw new ConflictException("Ya existe un tipo con ese nombre en la categoría.");
    }
    if (currentId != null) {
      assetTypeRepository.findWithCategoryAndAttributesById(currentId).ifPresent(existing -> {
        boolean duplicatedInSameCategory =
            existing.getCategory().getId().equals(categoryId)
                && !existing.getName().equalsIgnoreCase(normalizedName)
                && assetTypeRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, normalizedName);
        boolean duplicatedInOtherCategory =
            !existing.getCategory().getId().equals(categoryId)
                && assetTypeRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, normalizedName);
        if (duplicatedInSameCategory || duplicatedInOtherCategory) {
          throw new ConflictException("Ya existe un tipo con ese nombre en la categoría.");
        }
      });
    }
  }

  private List<AssetAttributeDefinition> mergeAttributes(
      AssetType assetType,
      List<AttributeDefinitionRequest> requests
  ) {
    Map<UUID, AssetAttributeDefinition> existingById = new LinkedHashMap<>();
    for (AssetAttributeDefinition attribute : assetType.getAttributes()) {
      existingById.put(attribute.getId(), attribute);
    }

    return requests.stream().map(request -> {
      AssetAttributeDefinition attribute = request.id() != null
          ? existingById.getOrDefault(request.id(), toAttributeEntity(request))
          : toAttributeEntity(request);

      attribute.setName(request.name().trim());
      attribute.setDescription(request.description().trim());
      attribute.setRequired(request.isRequired());
      attribute.setAssetType(assetType);
      return attribute;
    }).toList();
  }

  private AssetAttributeDefinition toAttributeEntity(AttributeDefinitionRequest request) {
    return new AssetAttributeDefinition(
        request.name().trim(),
        request.description().trim(),
        request.isRequired()
    );
  }

  private String normalizeName(String name) {
    return name.trim().toLowerCase(Locale.ROOT);
  }
}
