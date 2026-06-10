package com.sigae.api.model.dto;

import java.util.List;

public record LoanReturnRequest(
    List<LoanReturnAssetReviewRequest> assetReviews
) {}
