package com.sigae.api.model.dto;

import com.sigae.api.model.entity.AssetAttributeDefinition;
import java.util.UUID;

public record AttributeDefinitionResponse(
    UUID id,
    String name,
    String description,
    boolean isRequired
) {

  public static AttributeDefinitionResponse from(AssetAttributeDefinition attribute) {
    return new AttributeDefinitionResponse(
        attribute.getId(),
        attribute.getName(),
        attribute.getDescription(),
        attribute.isRequired()
    );
  }
}
