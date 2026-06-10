package com.sigae.api.service;

import com.sigae.api.exception.BadRequestException;
import com.sigae.api.exception.ConflictException;
import com.sigae.api.exception.NotFoundException;
import com.sigae.api.model.dto.AssetAttributeValueRequest;
import com.sigae.api.model.dto.AssetAttachmentFile;
import com.sigae.api.model.dto.AssetInventoryGroupResponse;
import com.sigae.api.model.dto.AssetInventoryGroupUnitResponse;
import com.sigae.api.model.dto.AssetRequest;
import com.sigae.api.model.entity.AssetAttachment;
import com.sigae.api.model.entity.Asset;
import com.sigae.api.model.entity.AssetAttributeDefinition;
import com.sigae.api.model.entity.AssetAttributeValue;
import com.sigae.api.model.entity.AssetCondition;
import com.sigae.api.model.entity.AssetTraceability;
import com.sigae.api.model.entity.AssetType;
import com.sigae.api.model.entity.Location;
import com.sigae.api.model.entity.Supplier;
import com.sigae.api.model.entity.TraceabilityEventType;
import com.sigae.api.repository.AssetRepository;
import com.sigae.api.repository.AssetAttachmentRepository;
import com.sigae.api.repository.AssetTraceabilityRepository;
import com.sigae.api.repository.AssetTypeRepository;
import com.sigae.api.repository.LocationRepository;
import com.sigae.api.repository.SupplierRepository;
import java.io.IOException;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class AssetService {
  private static final Map<String, String> TYPE_PREFIXES = buildTypePrefixes();
  private static final Map<String, String> CATEGORY_PREFIXES = buildCategoryPrefixes();

  private final AssetRepository assetRepository;
  private final AssetAttachmentRepository attachmentRepository;
  private final AssetTypeRepository assetTypeRepository;
  private final LocationRepository locationRepository;
  private final SupplierRepository supplierRepository;
  private final AssetTraceabilityRepository traceabilityRepository;
  private final LoanService loanService;

  public AssetService(
      AssetRepository assetRepository,
      AssetAttachmentRepository attachmentRepository,
      AssetTypeRepository assetTypeRepository,
      LocationRepository locationRepository,
      SupplierRepository supplierRepository,
      AssetTraceabilityRepository traceabilityRepository,
      LoanService loanService
  ) {
    this.assetRepository = assetRepository;
    this.attachmentRepository = attachmentRepository;
    this.assetTypeRepository = assetTypeRepository;
    this.locationRepository = locationRepository;
    this.supplierRepository = supplierRepository;
    this.traceabilityRepository = traceabilityRepository;
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
    return assetRepository.findAll().stream().map(this::toResponse).toList();
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
        .or(() -> assetRepository.findByBarcodeIgnoreCase(normalizedValue))
        .orElseThrow(() -> new NotFoundException("No se encontró un activo con el valor escaneado."));
  }

  public List<AssetTraceability> getTraceability(UUID assetId) {
    getById(assetId);
    return traceabilityRepository.findByAssetIdOrderByOccurredAtDesc(assetId);
  }

  @Transactional
  public Asset create(AssetRequest request, List<MultipartFile> attachments) {
    AssetType assetType = getAssetType(request.assetTypeId());
    Location location = getLocation(request.locationId());
    Supplier supplier = getSupplierOrNull(request.supplierId());
    String resolvedCode = resolveCodeForCreate(request, assetType);
    String resolvedBarcode = resolveBarcodeForCreate(request, resolvedCode);

    ensureCodeAvailable(resolvedCode, null);
    ensureBarcodeAvailable(resolvedBarcode, null);

    Asset asset = new Asset(
        resolvedCode,
        request.name().trim(),
        assetType,
        location,
        supplier,
        request.condition()
    );
    applyOptionalFields(asset, request, resolvedBarcode);
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
        null
    ));
    return getById(saved.getId());
  }

  @Transactional
  public Asset update(UUID id, AssetRequest request, List<MultipartFile> attachments) {
    Asset asset = getById(id);
    AssetCondition previousCondition = asset.getCondition();
    UUID previousLocationId = asset.getLocation().getId();

    AssetType assetType = getAssetType(request.assetTypeId());
    Location location = getLocation(request.locationId());
    String resolvedCode = requireExistingCode(request.code());
    String resolvedBarcode = resolveBarcodeForUpdate(request, asset, resolvedCode);

    ensureCodeAvailable(resolvedCode, asset.getId());
    ensureBarcodeAvailable(resolvedBarcode, asset.getId());

    asset.setCode(resolvedCode);
    asset.setName(request.name().trim());
    asset.setAssetType(assetType);
    asset.setLocation(location);
    asset.setSupplier(getSupplierOrNull(request.supplierId()));
    asset.setCondition(request.condition());
    applyOptionalFields(asset, request, resolvedBarcode);
    asset.syncAttributeValues(buildAttributeValues(assetType, request.attributeValues()));
    removeAttachments(asset, request.removedAttachmentIds());
    applyAttachments(asset, attachments);

    Asset saved = assetRepository.save(asset);
    traceabilityRepository.save(new AssetTraceability(
        saved,
        TraceabilityEventType.UPDATED,
        "Activo actualizado.",
        null,
        saved.getCode(),
        null,
        null
    ));

    if (previousCondition != saved.getCondition()) {
      traceabilityRepository.save(new AssetTraceability(
          saved,
          saved.getCondition() == AssetCondition.DADO_DE_BAJA ? TraceabilityEventType.DECOMMISSIONED : TraceabilityEventType.CONDITION_CHANGED,
          "Condición del activo actualizada.",
          previousCondition.getLabel(),
          saved.getCondition().getLabel(),
          saved.getNotes(),
          null
      ));
    }

    if (!previousLocationId.equals(saved.getLocation().getId())) {
      traceabilityRepository.save(new AssetTraceability(
          saved,
          TraceabilityEventType.LOCATION_CHANGED,
          "Ubicación del activo actualizada.",
          previousLocationId.toString(),
          saved.getLocation().getId().toString(),
          null,
          null
      ));
    }

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

  private void applyOptionalFields(Asset asset, AssetRequest request, String resolvedBarcode) {
    asset.setSerialNumber(normalizeOptional(request.serialNumber()));
    asset.setBarcode(resolvedBarcode);
    asset.setAcquisitionDate(request.acquisitionDate());
    asset.setNotes(normalizeOptional(request.notes()));
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

  private void ensureBarcodeAvailable(String barcode, UUID currentId) {
    String normalizedBarcode = normalizeOptional(barcode);
    if (normalizedBarcode == null) {
      return;
    }

    assetRepository.findByBarcodeIgnoreCase(normalizedBarcode)
        .filter(asset -> !asset.getId().equals(currentId))
        .ifPresent(asset -> {
          throw new ConflictException("Ya existe un activo con ese código de barras.");
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

  private String resolveBarcodeForCreate(AssetRequest request, String resolvedCode) {
    String normalizedBarcode = normalizeOptional(request.barcode());
    return normalizedBarcode != null ? normalizedBarcode : resolvedCode;
  }

  private String resolveBarcodeForUpdate(AssetRequest request, Asset asset, String resolvedCode) {
    if (request.barcode() != null) {
      return resolveBarcodeForCreate(request, resolvedCode);
    }

    String existingBarcode = normalizeOptional(asset.getBarcode());
    return existingBarcode != null ? existingBarcode : resolvedCode;
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
            asset.getAssetType().getCategory().getName()
        )));

    return groupedAssets.entrySet().stream()
        .map(entry -> new AssetInventoryGroupResponse(
            entry.getKey().groupId(),
            entry.getKey().displayName(),
            entry.getKey().categoryId(),
            entry.getKey().categoryIcon(),
            entry.getKey().categoryName(),
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
      String categoryName
  ) {}
}
