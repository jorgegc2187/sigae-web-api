package com.sigae.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "loan_asset")
public class LoanAsset extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "loan_id", nullable = false)
  private Loan loan;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "asset_id", nullable = false)
  private Asset asset;

  @Column(nullable = false, length = 30)
  private String assetCodeSnapshot;

  @Column(nullable = false, length = 160)
  private String assetNameSnapshot;

  @Column(nullable = false, length = 150)
  private String assetCategorySnapshot;

  protected LoanAsset() {}

  public LoanAsset(Asset asset) {
    this.asset = asset;
    this.assetCodeSnapshot = asset.getCode();
    this.assetNameSnapshot = asset.getName();
    this.assetCategorySnapshot = asset.getAssetType().getCategory().getName();
  }

  void setLoan(Loan loan) {
    this.loan = loan;
  }

  public Loan getLoan() {
    return loan;
  }

  public Asset getAsset() {
    return asset;
  }

  public String getAssetCodeSnapshot() {
    return assetCodeSnapshot;
  }

  public String getAssetNameSnapshot() {
    return assetNameSnapshot;
  }

  public String getAssetCategorySnapshot() {
    return assetCategorySnapshot;
  }
}
