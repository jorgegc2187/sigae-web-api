package com.sigae.api.model.dto;

import com.sigae.api.model.entity.AssetAttributeValue;
import java.util.UUID;

public record AssetAttributeValueResponse(
    UUID id,
    UUID attributeDefinitionId,
    String attributeName,
    String value
) {
  public static AssetAttributeValueResponse from(AssetAttributeValue value) {
    return new AssetAttributeValueResponse(
        value.getId(),
        value.getAttributeDefinition().getId(),
        value.getAttributeDefinition().getName(),
        value.getValue()
    );
  }
}
