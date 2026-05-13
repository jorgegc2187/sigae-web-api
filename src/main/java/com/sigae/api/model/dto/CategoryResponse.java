package com.sigae.api.model.dto;

import com.sigae.api.model.entity.Category;
import java.util.List;
import java.util.UUID;

public record CategoryResponse(
    UUID id,
    String name,
    String icon,
    int typesCount,
    long assetsCount,
    List<AssetTypeResponse> types
) {

  public static CategoryResponse from(Category category) {
    return new CategoryResponse(
        category.getId(),
        category.getName(),
        category.getIcon(),
        category.getTypes().size(),
        0,
        category.getTypes().stream().map(AssetTypeResponse::from).toList()
    );
  }
}
