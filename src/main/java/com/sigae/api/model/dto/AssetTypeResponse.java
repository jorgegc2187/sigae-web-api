package com.sigae.api.model.dto;

import com.sigae.api.model.entity.AssetType;
import java.util.List;
import java.util.UUID;

public record AssetTypeResponse(
    UUID id,
    String name,
    String icon,
    List<AttributeDefinitionResponse> attributes
) {

  public static AssetTypeResponse from(AssetType assetType) {
    return new AssetTypeResponse(
        assetType.getId(),
        assetType.getName(),
        assetType.getIcon(),
        assetType.getAttributes().stream().map(AttributeDefinitionResponse::from).toList()
    );
  }
}
