package com.sigae.api.service;

import com.sigae.api.exception.BadRequestException;
import com.sigae.api.exception.ConflictException;
import com.sigae.api.exception.NotFoundException;
import com.sigae.api.model.dto.AssetAttributeValueRequest;
import com.sigae.api.model.dto.AssetAttachmentFile;
import com.sigae.api.model.dto.AssetInventoryGroupResponse;
import com.sigae.api.model.dto.AssetInventoryGroupUnitResponse;
import com.sigae.api.model.dto.PageResponse;
import com.sigae.api.model.dto.AssetRequest;
import com.sigae.api.model.dto.AssetStatusChangeRequest;
import com.sigae.api.model.entity.AssetAttachment;
import com.sigae.api.model.entity.Asset;
import com.sigae.api.model.entity.AssetAttributeDefinition;
import com.sigae.api.model.entity.AssetAttributeValue;
import com.sigae.api.model.entity.AssetCondition;
import com.sigae.api.model.entity.AssetTraceability;
import com.sigae.api.model.entity.AssetTraceabilityAttachment;
import com.sigae.api.model.entity.AssetType;
import com.sigae.api.model.entity.Location;
import com.sigae.api.model.entity.Supplier;
import com.sigae.api.model.entity.TraceabilityEventType;
import com.sigae.api.model.entity.User;
import com.sigae.api.repository.AssetRepository;
import com.sigae.api.repository.AssetAttachmentRepository;
import com.sigae.api.repository.AssetTraceabilityAttachmentRepository;
import com.sigae.api.repository.AssetTraceabilityRepository;
import com.sigae.api.repository.AssetTypeRepository;
import com.sigae.api.repository.LocationRepository;
import com.sigae.api.repository.SupplierRepository;
import com.sigae.api.repository.UserRepository;
import com.sigae.api.security.AuthenticatedUser;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class AssetService {
  private static final Map<String, String> TYPE_PREFIXES = buildTypePrefixes();
  private static final Map<String, String> CATEGORY_PREFIXES = buildCategoryPrefixes();
  private static final long STATUS_CHANGE_ATTACHMENT_MAX_SIZE_BYTES = 5L * 1024 * 1024;
  private static final Set<String> STATUS_CHANGE_ALLOWED_CONTENT_TYPES = Set.of(
      MediaType.IMAGE_JPEG_VALUE,
      "image/jpg",
      MediaType.IMAGE_PNG_VALUE,
      MediaType.APPLICATION_PDF_VALUE
  );
  private static final Set<String> STATUS_CHANGE_ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "pdf");

  private final AssetRepository assetRepository;
  private final AssetAttachmentRepository attachmentRepository;
  private final AssetTraceabilityAttachmentRepository traceabilityAttachmentRepository;
  private final AssetTypeRepository assetTypeRepository;
  private final LocationRepository locationRepository;
  private final SupplierRepository supplierRepository;
  private final AssetTraceabilityRepository traceabilityRepository;
  private final UserRepository userRepository;
  private final LoanService loanService;

  public AssetService(
      AssetRepository assetRepository,
      AssetAttachmentRepository attachmentRepository,
      AssetTraceabilityAttachmentRepository traceabilityAttachmentRepository,
      AssetTypeRepository assetTypeRepository,
      LocationRepository locationRepository,
      SupplierRepository supplierRepository,
      AssetTraceabilityRepository traceabilityRepository,
      UserRepository userRepository,
      LoanService loanService
  ) {
    this.assetRepository = assetRepository;
    this.attachmentRepository = attachmentRepository;
    this.traceabilityAttachmentRepository = traceabilityAttachmentRepository;
    this.assetTypeRepository = assetTypeRepository;
    this.locationRepository = locationRepository;
    this.supplierRepository = supplierRepository;
    this.traceabilityRepository = traceabilityRepository;
    this.userRepository = userRepository;
    this.loanService = loanService;
  }

  public List<Asset> findAll() {
    return assetRepository.findAll();
  }

  public com.sigae.api.model.dto.AssetResponse toResponse(Asset asset) {
    UUID activeLoanId = loanService.activeLoanIdForAsset(asset.getId());
    return com.sigae.api.model.dto.AssetResponse.from(asset, activeLoanId == null && loanService.isAssetAvailableForLoan(asset), activeLoanId);
  }

  public List<com.sigae.api.model.dto.AssetResponse> findAllResponses() {
    return assetRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
        .map(this::toResponse)
        .toList();
  }

  public PageResponse<com.sigae.api.model.dto.AssetResponse> findPageResponses(
      String search,
      UUID categoryId,
      AssetCondition condition,
      UUID locationId,
      int page,
      int size,
      String sortDirection
  ) {
    if (page < 1 || size < 1 || size > 100) {
      throw new BadRequestException("Los parámetros de paginación no son válidos.");
    }

    Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
        ? Sort.Direction.ASC
        : Sort.Direction.DESC;
    PageRequest pageable = PageRequest.of(
        page - 1,
        size,
        Sort.by(direction, "createdAt").and(Sort.by(Sort.Direction.ASC, "id"))
    );
    Page<Asset> result = assetRepository.findAll(buildPageSpecification(search, categoryId, condition, locationId), pageable);

    return new PageResponse<>(
        result.getContent().stream().map(this::toResponse).toList(),
        result.getNumber() + 1,
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages(),
        result.hasNext(),
        result.hasPrevious()
    );
  }

  public com.sigae.api.model.dto.AssetResponse getResponseById(UUID id) {
    return toResponse(getById(id));
  }

  public com.sigae.api.model.dto.AssetResponse lookupResponseByScanValue(String value) {
    return toResponse(lookupByScanValue(value));
  }

  public List<AssetInventoryGroupResponse> findGrouped(String search, UUID categoryId) {
    return buildGroupedResponses(search, categoryId);
  }

  public AssetInventoryGroupResponse findGroupedById(String groupId) {
    return buildGroupedResponses(null, null).stream()
        .filter(group -> group.groupId().equals(groupId))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Familia de activos no encontrada."));
  }

  public Asset getById(UUID id) {
    return assetRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Activo no encontrado."));
  }

  public Asset lookupByScanValue(String value) {
    String normalizedValue = normalizeOptional(value);
    if (normalizedValue == null) {
      throw new NotFoundException("No se encontró un activo con el valor escaneado.");
    }

    return assetRepository.findByCodeIgnoreCase(normalizedValue)
        .orElseThrow(() -> new NotFoundException("No se encontró un activo con el valor escaneado."));
  }

  public List<AssetTraceability> getTraceability(UUID assetId) {
    getById(assetId);
    return traceabilityRepository.findByAssetIdOrderByOccurredAtDesc(assetId);
  }

  @Transactional
  public Asset changeStatus(
      UUID assetId,
      AssetStatusChangeRequest request,
      List<MultipartFile> attachments,
      AuthenticatedUser authenticatedUser
  ) {
    Asset asset = getById(assetId);
    User user = findUser(authenticatedUser);
    AssetCondition previousCondition = asset.getCondition();
    AssetCondition nextCondition = request.nextCondition();
    String reason = normalizeOptional(request.reason());

    if (reason == null) {
      throw new BadRequestException("El motivo del cambio de estado es obligatorio.");
    }

    if (previousCondition == nextCondition) {
      throw new BadRequestException("El activo ya se encuentra en el estado seleccionado.");
    }

    validateStatusChangeAttachments(attachments);

    asset.setCondition(nextCondition);
    applyDecommissionedAtOnUpdate(asset, previousCondition, nextCondition);
    Asset saved = assetRepository.save(asset);

    TraceabilityEventType eventType = resolveConditionTraceabilityEvent(previousCondition, nextCondition);
    AssetTraceability traceability = new AssetTraceability(
        saved,
        eventType,
        conditionTraceabilityDescription(eventType),
        previousCondition.getLabel(),
        nextCondition.getLabel(),
        reason,
        user
    );
    applyTraceabilityAttachments(traceability, attachments);
    traceabilityRepository.save(traceability);

    return getById(saved.getId());
  }

  @Transactional
  public Asset create(AssetRequest request, List<MultipartFile> attachments, AuthenticatedUser authenticatedUser) {
    User user = findUser(authenticatedUser);
    AssetType assetType = getAssetType(request.assetTypeId());
    Location location = getLocation(request.locationId());
    Supplier supplier = getSupplierOrNull(request.supplierId());
    String resolvedCode = resolveCodeForCreate(request, assetType);

    ensureCodeAvailable(resolvedCode, null);

    Asset asset = new Asset(
        resolvedCode,
        request.name().trim(),
        assetType,
        location,
        supplier,
        request.condition()
    );
    asset.setCreatedBy(user);
    applyOptionalFields(asset, request);
    applyDecommissionedAtOnCreate(asset);
    asset.syncAttributeValues(buildAttributeValues(assetType, request.attributeValues()));
    applyAttachments(asset, attachments);

    Asset saved = assetRepository.save(asset);
    traceabilityRepository.save(new AssetTraceability(
        saved,
        TraceabilityEventType.CREATED,
        "Activo registrado en el inventario.",
        null,
        saved.getCode(),
        null,
        user
    ));
    if (saved.getCondition() == AssetCondition.DADO_DE_BAJA) {
      traceabilityRepository.save(new AssetTraceability(
          saved,
          TraceabilityEventType.DECOMMISSIONED,
          "Activo dado de baja.",
          null,
          saved.getCondition().getLabel(),
          saved.getNotes(),
          user
      ));
    }
    return getById(saved.getId());
  }

  @Transactional
  public Asset update(UUID id, AssetRequest request, List<MultipartFile> attachments, AuthenticatedUser authenticatedUser) {
    Asset asset = getById(id);
    User user = findUser(authenticatedUser);

    String previousName = asset.getName();
    String previousCode = asset.getCode();
    String previousSerialNumber = asset.getSerialNumber();
    java.time.LocalDate previousAcquisitionDate = asset.getAcquisitionDate();
    String previousNotes = asset.getNotes();
    AssetCondition previousCondition = asset.getCondition();
    String previousLocationName = asset.getLocation().getName();
    String previousSupplierName = asset.getSupplier() == null ? null : asset.getSupplier().getName();
    String previousTypeName = asset.getAssetType().getName();
    String previousCategoryName = asset.getAssetType().getCategory().getName();
    Map<UUID, AttributeSnapshot> previousAttributes = asset.getAttributeValues().stream()
        .collect(Collectors.toMap(
            value -> value.getAttributeDefinition().getId(),
            value -> new AttributeSnapshot(
                value.getAttributeDefinition().getId(),
                value.getAttributeDefinition().getName(),
                value.getValue()
            )
        ));

    AssetType assetType = getAssetType(request.assetTypeId());
    Location location = getLocation(request.locationId());
    Supplier supplier = getSupplierOrNull(request.supplierId());
    String resolvedCode = requireExistingCode(request.code());
    AssetCondition nextCondition = request.condition();
    List<AssetAttributeValue> nextAttributeValues = buildAttributeValues(assetType, request.attributeValues());
    Map<UUID, AttributeSnapshot> nextAttributes = nextAttributeValues.stream()
        .collect(Collectors.toMap(
            value -> value.getAttributeDefinition().getId(),
            value -> new AttributeSnapshot(
                value.getAttributeDefinition().getId(),
                value.getAttributeDefinition().getName(),
                value.getValue()
            ),
            (left, right) -> right
        ));

    ensureCodeAvailable(resolvedCode, asset.getId());

    asset.setCode(resolvedCode);
    asset.setName(request.name().trim());
    asset.setAssetType(assetType);
    asset.setLocation(location);
    asset.setSupplier(supplier);
    asset.setCondition(nextCondition);
    applyOptionalFields(asset, request);
    applyDecommissionedAtOnUpdate(asset, previousCondition, nextCondition);
    asset.syncAttributeValues(nextAttributeValues);
    removeAttachments(asset, request.removedAttachmentIds());
    applyAttachments(asset, attachments);

    Asset saved = assetRepository.save(asset);
    registerFieldTraceability(saved, TraceabilityEventType.UPDATED, "Nombre del activo actualizado.", previousName, saved.getName(), null, user);
    registerFieldTraceability(saved, TraceabilityEventType.UPDATED, "Codigo del activo actualizado.", previousCode, saved.getCode(), null, user);
    registerFieldTraceability(saved, TraceabilityEventType.UPDATED, "Proveedor del activo actualizado.", previousSupplierName, supplier == null ? null : supplier.getName(), null, user);
    registerFieldTraceability(saved, TraceabilityEventType.UPDATED, "Serial number del activo actualizado.", previousSerialNumber, saved.getSerialNumber(), null, user);
    registerFieldTraceability(
        saved,
        TraceabilityEventType.UPDATED,
        "Fecha de adquisicion del activo actualizada.",
        formatLocalDate(previousAcquisitionDate),
        formatLocalDate(saved.getAcquisitionDate()),
        null,
        user
    );
    registerFieldTraceability(saved, TraceabilityEventType.UPDATED, "Notas del activo actualizadas.", previousNotes, saved.getNotes(), null, user);
    registerFieldTraceability(saved, TraceabilityEventType.UPDATED, "Tipo de activo actualizado.", previousTypeName, saved.getAssetType().getName(), null, user);
    registerFieldTraceability(saved, TraceabilityEventType.UPDATED, "Categoria del activo actualizada.", previousCategoryName, saved.getAssetType().getCategory().getName(), null, user);

    if (previousCondition != saved.getCondition()) {
      TraceabilityEventType eventType = resolveConditionTraceabilityEvent(previousCondition, saved.getCondition());
      traceabilityRepository.save(new AssetTraceability(
          saved,
          eventType,
          conditionTraceabilityDescription(eventType),
          previousCondition.getLabel(),
          saved.getCondition().getLabel(),
          saved.getNotes(),
          user
      ));
    }

    if (!Objects.equals(previousLocationName, saved.getLocation().getName())) {
      traceabilityRepository.save(new AssetTraceability(
          saved,
          TraceabilityEventType.LOCATION_CHANGED,
          "Ubicación del activo actualizada.",
          previousLocationName,
          saved.getLocation().getName(),
          null,
          user
      ));
    }

    registerAttributeTraceability(saved, previousAttributes, nextAttributes, user);

    return getById(saved.getId());
  }

  private AssetType getAssetType(UUID id) {
    return assetTypeRepository.findWithCategoryAndAttributesById(id)
        .orElseThrow(() -> new NotFoundException("Tipo de activo no encontrado."));
  }

  private Location getLocation(UUID id) {
    return locationRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Ubicación no encontrada."));
  }

  private Supplier getSupplierOrNull(UUID id) {
    if (id == null) {
      return null;
    }
    return supplierRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Proveedor no encontrado."));
  }

  private void applyOptionalFields(Asset asset, AssetRequest request) {
    asset.setSerialNumber(normalizeOptional(request.serialNumber()));
    asset.setAcquisitionDate(request.acquisitionDate());
    asset.setNotes(normalizeOptional(request.notes()));
  }

  private void applyDecommissionedAtOnCreate(Asset asset) {
    asset.setDecommissionedAt(asset.getCondition() == AssetCondition.DADO_DE_BAJA ? Instant.now() : null);
  }

  private void applyDecommissionedAtOnUpdate(Asset asset, AssetCondition previousCondition, AssetCondition nextCondition) {
    if (previousCondition != AssetCondition.DADO_DE_BAJA && nextCondition == AssetCondition.DADO_DE_BAJA) {
      asset.setDecommissionedAt(Instant.now());
      return;
    }

    if (previousCondition == AssetCondition.DADO_DE_BAJA && nextCondition != AssetCondition.DADO_DE_BAJA) {
      asset.setDecommissionedAt(null);
    }
  }

  private TraceabilityEventType resolveConditionTraceabilityEvent(AssetCondition previousCondition, AssetCondition nextCondition) {
    if (previousCondition != AssetCondition.DADO_DE_BAJA && nextCondition == AssetCondition.DADO_DE_BAJA) {
      return TraceabilityEventType.DECOMMISSIONED;
    }

    if (previousCondition == AssetCondition.DADO_DE_BAJA && nextCondition != AssetCondition.DADO_DE_BAJA) {
      return TraceabilityEventType.REACTIVATED;
    }

    return TraceabilityEventType.CONDITION_CHANGED;
  }

  private String conditionTraceabilityDescription(TraceabilityEventType eventType) {
    return switch (eventType) {
      case DECOMMISSIONED -> "Activo dado de baja.";
      case REACTIVATED -> "Activo reactivado.";
      default -> "Condición del activo actualizada.";
    };
  }

  private void registerAttributeTraceability(
      Asset asset,
      Map<UUID, AttributeSnapshot> previousAttributes,
      Map<UUID, AttributeSnapshot> nextAttributes,
      User user
  ) {
    Map<UUID, AttributeSnapshot> mergedAttributes = new HashMap<>(previousAttributes);
    mergedAttributes.putAll(nextAttributes);

    mergedAttributes.forEach((attributeId, snapshot) -> {
      AttributeSnapshot previous = previousAttributes.get(attributeId);
      AttributeSnapshot next = nextAttributes.get(attributeId);
      String previousValue = previous == null ? null : previous.value();
      String newValue = next == null ? null : next.value();

      if (Objects.equals(normalizeOptional(previousValue), normalizeOptional(newValue))) {
        return;
      }

      traceabilityRepository.save(new AssetTraceability(
          asset,
          TraceabilityEventType.UPDATED,
          "Atributo \"%s\" actualizado.".formatted(snapshot.name()),
          previousValue,
          newValue,
          null,
          user
      ));
    });
  }

  private void registerFieldTraceability(
      Asset asset,
      TraceabilityEventType eventType,
      String description,
      String previousValue,
      String newValue,
      String reason,
      User user
  ) {
    if (Objects.equals(normalizeOptional(previousValue), normalizeOptional(newValue))) {
      return;
    }

    traceabilityRepository.save(new AssetTraceability(
        asset,
        eventType,
        description,
        previousValue,
        newValue,
        reason,
        user
    ));
  }

  private User findUser(AuthenticatedUser authenticatedUser) {
    if (authenticatedUser == null) {
      return null;
    }

    return userRepository.findById(authenticatedUser.userId()).orElse(null);
  }

  public AssetAttachmentFile getAttachment(UUID assetId, UUID attachmentId) {
    AssetAttachment attachment = attachmentRepository.findByIdAndAssetId(attachmentId, assetId)
        .orElseThrow(() -> new NotFoundException("Adjunto de activo no encontrado."));
    return new AssetAttachmentFile(
        attachment.getFileName(),
        MediaType.parseMediaType(attachment.getMimeType()),
        attachment.getContent()
    );
  }

  public com.sigae.api.model.dto.AssetTraceabilityAttachmentFile getTraceabilityAttachment(
      UUID assetId,
      UUID traceabilityId,
      UUID attachmentId
  ) {
    AssetTraceabilityAttachment attachment = traceabilityAttachmentRepository
        .findByIdAndTraceabilityIdAndTraceabilityAssetId(attachmentId, traceabilityId, assetId)
        .orElseThrow(() -> new NotFoundException("Evidencia del cambio de estado no encontrada."));
    return new com.sigae.api.model.dto.AssetTraceabilityAttachmentFile(
        attachment.getFileName(),
        MediaType.parseMediaType(attachment.getMimeType()),
        attachment.getContent()
    );
  }

  private List<AssetAttributeValue> buildAttributeValues(
      AssetType assetType,
      List<AssetAttributeValueRequest> requests
  ) {
    if (requests == null || requests.isEmpty()) {
      return List.of();
    }

    Map<UUID, AssetAttributeDefinition> definitions = assetType.getAttributes().stream()
        .collect(Collectors.toMap(AssetAttributeDefinition::getId, Function.identity()));

    return requests.stream()
        .map(request -> {
          AssetAttributeDefinition definition = definitions.get(request.attributeDefinitionId());
          if (definition == null) {
            throw new NotFoundException("Atributo no pertenece al tipo de activo seleccionado.");
          }
          return new AssetAttributeValue(definition, request.value().trim());
        })
        .toList();
  }

  private void ensureCodeAvailable(String code, UUID currentId) {
    assetRepository.findByCodeIgnoreCase(code.trim())
        .filter(asset -> !asset.getId().equals(currentId))
        .ifPresent(asset -> {
          throw new ConflictException("Ya existe un activo con ese código.");
        });
  }

  private void applyAttachments(Asset asset, List<MultipartFile> attachments) {
    if (attachments == null || attachments.isEmpty()) {
      return;
    }

    for (int index = 0; index < attachments.size(); index++) {
      MultipartFile file = attachments.get(index);
      if (file == null || file.isEmpty()) {
        continue;
      }

      try {
        asset.addAttachment(new AssetAttachment(
            normalizeFilename(file.getOriginalFilename(), "adjunto-activo-%d".formatted(index + 1)),
            file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType(),
            file.getSize(),
            file.getBytes()
        ));
      } catch (IOException exception) {
        throw new BadRequestException("No se pudo procesar uno de los adjuntos del activo.");
      }
    }
  }

  private void applyTraceabilityAttachments(AssetTraceability traceability, List<MultipartFile> attachments) {
    if (attachments == null || attachments.isEmpty()) {
      return;
    }

    for (int index = 0; index < attachments.size(); index++) {
      MultipartFile file = attachments.get(index);
      if (file == null || file.isEmpty()) {
        continue;
      }

      try {
        traceability.addAttachment(new AssetTraceabilityAttachment(
            normalizeFilename(file.getOriginalFilename(), "evidencia-cambio-estado-%d".formatted(index + 1)),
            resolveStatusChangeMimeType(file),
            file.getSize(),
            file.getBytes()
        ));
      } catch (IOException exception) {
        throw new BadRequestException("No se pudo procesar una de las evidencias adjuntas.");
      }
    }
  }

  private void validateStatusChangeAttachments(List<MultipartFile> attachments) {
    if (attachments == null || attachments.isEmpty()) {
      return;
    }

    for (MultipartFile attachment : attachments) {
      if (attachment == null || attachment.isEmpty()) {
        continue;
      }

      if (attachment.getSize() > STATUS_CHANGE_ATTACHMENT_MAX_SIZE_BYTES) {
        throw new BadRequestException("Cada evidencia adjunta debe pesar como máximo 5MB.");
      }

      String mimeType = resolveStatusChangeMimeType(attachment);
      String extension = extractExtension(attachment.getOriginalFilename());
      if (!STATUS_CHANGE_ALLOWED_CONTENT_TYPES.contains(mimeType) && !STATUS_CHANGE_ALLOWED_EXTENSIONS.contains(extension)) {
        throw new BadRequestException("Solo se permiten evidencias JPG, PNG o PDF.");
      }
    }
  }

  private String resolveStatusChangeMimeType(MultipartFile file) {
    return normalizeOptional(file.getContentType()) == null
        ? MediaType.APPLICATION_OCTET_STREAM_VALUE
        : Objects.requireNonNull(file.getContentType()).trim().toLowerCase(Locale.ROOT);
  }

  private String extractExtension(String fileName) {
    String normalized = normalizeOptional(fileName);
    if (normalized == null || !normalized.contains(".")) {
      return "";
    }

    return normalized.substring(normalized.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
  }

  private void removeAttachments(Asset asset, List<UUID> removedAttachmentIds) {
    if (removedAttachmentIds == null || removedAttachmentIds.isEmpty()) {
      return;
    }

    Map<UUID, AssetAttachment> attachmentsById = asset.getAttachments().stream()
        .collect(Collectors.toMap(AssetAttachment::getId, Function.identity()));
    for (UUID removedAttachmentId : removedAttachmentIds) {
      AssetAttachment attachment = attachmentsById.get(removedAttachmentId);
      if (attachment == null) {
        throw new BadRequestException("Uno de los adjuntos a eliminar no pertenece al activo.");
      }
      asset.getAttachments().remove(attachment);
    }
  }

  private String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String normalizeFilename(String value, String fallback) {
    String normalized = normalizeOptional(value);
    return normalized == null ? fallback : normalized;
  }

  private String formatLocalDate(java.time.LocalDate value) {
    return value == null ? null : value.toString();
  }

  private String resolveCodeForCreate(AssetRequest request, AssetType assetType) {
    String providedCode = normalizeOptional(request.code());
    if (providedCode != null) {
      return providedCode;
    }

    return generateCode(assetType);
  }

  private String requireExistingCode(String code) {
    String normalizedCode = normalizeOptional(code);
    if (normalizedCode == null) {
      throw new BadRequestException("El código del activo es obligatorio.");
    }

    return normalizedCode;
  }

  private String generateCode(AssetType assetType) {
    String prefix = resolvePrefix(assetType);
    int year = java.time.LocalDate.now().getYear();
    String key = "%s-%s-".formatted(prefix, year);

    int nextSequence = assetRepository.findAllByCodeStartingWithIgnoreCase(key).stream()
        .map(Asset::getCode)
        .map(code -> extractSequence(code, key))
        .max(Integer::compareTo)
        .orElse(0) + 1;

    return "%s-%s-%03d".formatted(prefix, year, nextSequence);
  }

  private int extractSequence(String code, String key) {
    if (code == null || code.length() <= key.length()) {
      return 0;
    }

    try {
      return Integer.parseInt(code.substring(key.length()));
    } catch (NumberFormatException ignored) {
      return 0;
    }
  }

  private String resolvePrefix(AssetType assetType) {
    String typeName = assetType.getName().trim().toLowerCase(Locale.ROOT);
    if (TYPE_PREFIXES.containsKey(typeName)) {
      return TYPE_PREFIXES.get(typeName);
    }

    String categoryName = assetType.getCategory().getName().trim().toLowerCase(Locale.ROOT);
    if (CATEGORY_PREFIXES.containsKey(categoryName)) {
      return CATEGORY_PREFIXES.get(categoryName);
    }

    String normalized = assetType.getName().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    if (normalized.length() >= 3) {
      return normalized.substring(0, 3);
    }

    return (normalized + "AST").substring(0, 3);
  }

  private static Map<String, String> buildTypePrefixes() {
    Map<String, String> prefixes = new HashMap<>();
    prefixes.put("laptop", "CMP");
    prefixes.put("desktop", "DES");
    prefixes.put("proyector", "PRY");
    prefixes.put("router", "NET");
    prefixes.put("webcam", "VID");
    prefixes.put("impresora", "IMP");
    prefixes.put("tablet", "TAB");
    prefixes.put("monitor", "MON");
    prefixes.put("micrófono", "AUD");
    prefixes.put("microfono", "AUD");
    prefixes.put("cable hdmi", "ACC");
    prefixes.put("puntero láser", "ACC");
    prefixes.put("puntero laser", "ACC");
    prefixes.put("escritorio", "MOB");
    prefixes.put("silla", "MOB");
    prefixes.put("archivador", "MOB");
    prefixes.put("estante", "MOB");
    prefixes.put("microscopio", "LAB");
    prefixes.put("balanza digital", "LAB");
    prefixes.put("kit de química", "LAB");
    prefixes.put("kit de quimica", "LAB");
    prefixes.put("fuente de poder", "LAB");
    prefixes.put("balón", "DEP");
    prefixes.put("balon", "DEP");
    prefixes.put("cono", "DEP");
    prefixes.put("colchoneta", "DEP");
    prefixes.put("red", "DEP");
    return prefixes;
  }

  private static Map<String, String> buildCategoryPrefixes() {
    Map<String, String> prefixes = new HashMap<>();
    prefixes.put("tecnología", "TEC");
    prefixes.put("tecnologia", "TEC");
    prefixes.put("mobiliario", "MOB");
    prefixes.put("laboratorio", "LAB");
    prefixes.put("deportes", "DEP");
    return prefixes;
  }

  private String normalizeSearch(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private Specification<Asset> buildPageSpecification(
      String search,
      UUID categoryId,
      AssetCondition condition,
      UUID locationId
  ) {
    String normalizedSearch = normalizeSearch(search);
    return (root, query, criteriaBuilder) -> {
      var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
      if (!normalizedSearch.isBlank()) {
        String pattern = "%" + normalizedSearch + "%";
        var assetType = root.join("assetType");
        var category = assetType.join("category");
        var location = root.join("location");
        predicates.add(criteriaBuilder.or(
            criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
            criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern),
            criteriaBuilder.like(criteriaBuilder.lower(root.get("serialNumber")), pattern),
            criteriaBuilder.like(criteriaBuilder.lower(location.get("name")), pattern),
            criteriaBuilder.like(criteriaBuilder.lower(assetType.get("name")), pattern),
            criteriaBuilder.like(criteriaBuilder.lower(category.get("name")), pattern)
        ));
      }
      if (categoryId != null) {
        predicates.add(criteriaBuilder.equal(root.get("assetType").get("category").get("id"), categoryId));
      }
      if (condition != null) {
        predicates.add(criteriaBuilder.equal(root.get("condition"), condition));
      }
      if (locationId != null) {
        predicates.add(criteriaBuilder.equal(root.get("location").get("id"), locationId));
      }
      return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
    };
  }

  private List<AssetInventoryGroupResponse> buildGroupedResponses(String search, UUID categoryId) {
    String normalizedSearch = normalizeSearch(search);

    Map<AssetGroupKey, List<Asset>> groupedAssets = assetRepository.findAll().stream()
        .filter(asset -> categoryId == null || asset.getAssetType().getCategory().getId().equals(categoryId))
        .filter(asset -> matchesGroupedSearch(asset, normalizedSearch))
        .collect(Collectors.groupingBy(asset -> new AssetGroupKey(
            buildGroupId(asset),
            asset.getName().trim(),
            asset.getAssetType().getCategory().getId(),
            asset.getAssetType().getCategory().getIcon(),
            asset.getAssetType().getCategory().getName(),
            asset.getAssetType().getId(),
            asset.getAssetType().getName()
        )));

    return groupedAssets.entrySet().stream()
        .map(entry -> new AssetInventoryGroupResponse(
            entry.getKey().groupId(),
            entry.getKey().displayName(),
            entry.getKey().categoryId(),
            entry.getKey().categoryIcon(),
            entry.getKey().categoryName(),
            entry.getKey().typeId(),
            entry.getKey().typeName(),
            entry.getValue().size(),
            entry.getValue().stream()
                .map(Asset::getCreatedAt)
                .max(Comparator.naturalOrder())
                .orElse(null),
            entry.getValue().stream()
                .sorted(Comparator.comparing(Asset::getCode, String.CASE_INSENSITIVE_ORDER))
                .map(AssetInventoryGroupUnitResponse::from)
                .toList()
        ))
        .sorted(Comparator.comparing(AssetInventoryGroupResponse::displayName, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  private boolean matchesGroupedSearch(Asset asset, String normalizedSearch) {
    if (normalizedSearch.isBlank()) {
      return true;
    }

    return List.of(
            asset.getName(),
            asset.getCode(),
            asset.getSerialNumber(),
            asset.getLocation().getName(),
            asset.getAssetType().getName(),
            asset.getAssetType().getCategory().getName()
        ).stream()
        .filter(value -> value != null && !value.isBlank())
        .map(value -> value.toLowerCase(Locale.ROOT))
        .anyMatch(value -> value.contains(normalizedSearch));
  }

  private String buildGroupId(Asset asset) {
    String seed = "%s|%s".formatted(
        asset.getAssetType().getId(),
        asset.getName().trim().toLowerCase(Locale.ROOT)
    );
    return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
  }

  private record AssetGroupKey(
      String groupId,
      String displayName,
      UUID categoryId,
      String categoryIcon,
      String categoryName,
      UUID typeId,
      String typeName
  ) {}

  private record AttributeSnapshot(
      UUID id,
      String name,
      String value
  ) {}
}
