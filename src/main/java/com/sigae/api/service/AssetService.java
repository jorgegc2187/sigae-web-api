package com.sigae.api.service;

import com.sigae.api.exception.ConflictException;
import com.sigae.api.exception.NotFoundException;
import com.sigae.api.model.dto.AssetAttributeValueRequest;
import com.sigae.api.model.dto.AssetRequest;
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
import com.sigae.api.repository.AssetTraceabilityRepository;
import com.sigae.api.repository.AssetTypeRepository;
import com.sigae.api.repository.LocationRepository;
import com.sigae.api.repository.SupplierRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssetService {

  private final AssetRepository assetRepository;
  private final AssetTypeRepository assetTypeRepository;
  private final LocationRepository locationRepository;
  private final SupplierRepository supplierRepository;
  private final AssetTraceabilityRepository traceabilityRepository;

  public AssetService(
      AssetRepository assetRepository,
      AssetTypeRepository assetTypeRepository,
      LocationRepository locationRepository,
      SupplierRepository supplierRepository,
      AssetTraceabilityRepository traceabilityRepository
  ) {
    this.assetRepository = assetRepository;
    this.assetTypeRepository = assetTypeRepository;
    this.locationRepository = locationRepository;
    this.supplierRepository = supplierRepository;
    this.traceabilityRepository = traceabilityRepository;
  }

  public List<Asset> findAll() {
    return assetRepository.findAll();
  }

  public Asset getById(UUID id) {
    return assetRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Activo no encontrado."));
  }

  public List<AssetTraceability> getTraceability(UUID assetId) {
    getById(assetId);
    return traceabilityRepository.findByAssetIdOrderByOccurredAtDesc(assetId);
  }

  @Transactional
  public Asset create(AssetRequest request) {
    ensureCodeAvailable(request.code(), null);
    ensureBarcodeAvailable(request.barcode(), null);

    AssetType assetType = getAssetType(request.assetTypeId());
    Location location = getLocation(request.locationId());
    Supplier supplier = getSupplierOrNull(request.supplierId());

    Asset asset = new Asset(
        request.code().trim(),
        request.name().trim(),
        assetType,
        location,
        supplier,
        request.condition()
    );
    applyOptionalFields(asset, request);
    asset.replaceAttributeValues(buildAttributeValues(assetType, request.attributeValues()));

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
  public Asset update(UUID id, AssetRequest request) {
    Asset asset = getById(id);
    ensureCodeAvailable(request.code(), asset.getId());
    ensureBarcodeAvailable(request.barcode(), asset.getId());

    AssetCondition previousCondition = asset.getCondition();
    UUID previousLocationId = asset.getLocation().getId();

    AssetType assetType = getAssetType(request.assetTypeId());
    Location location = getLocation(request.locationId());

    asset.setCode(request.code().trim());
    asset.setName(request.name().trim());
    asset.setAssetType(assetType);
    asset.setLocation(location);
    asset.setSupplier(getSupplierOrNull(request.supplierId()));
    asset.setCondition(request.condition());
    applyOptionalFields(asset, request);
    asset.replaceAttributeValues(buildAttributeValues(assetType, request.attributeValues()));

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

  private void applyOptionalFields(Asset asset, AssetRequest request) {
    asset.setSerialNumber(normalizeOptional(request.serialNumber()));
    asset.setBarcode(normalizeOptional(request.barcode()));
    asset.setAcquisitionDate(request.acquisitionDate());
    asset.setNotes(normalizeOptional(request.notes()));
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

  private String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
