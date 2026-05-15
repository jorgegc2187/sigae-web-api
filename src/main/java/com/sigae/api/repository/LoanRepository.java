package com.sigae.api.repository;

import com.sigae.api.model.entity.Loan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

  @Override
  @EntityGraph(attributePaths = {"teacher", "destinationLocation", "assets", "assets.asset", "assets.asset.assetType", "assets.asset.assetType.category"})
  List<Loan> findAll();

  @Override
  @EntityGraph(attributePaths = {"teacher", "destinationLocation", "assets", "assets.asset", "assets.asset.assetType", "assets.asset.assetType.category"})
  Optional<Loan> findById(UUID id);

  @Query("""
      select distinct loan
      from Loan loan
      left join loan.assets loanAsset
      where (
        :search is null
        or lower(loan.code) like lower(concat('%', :search, '%'))
        or lower(loan.teacherNameSnapshot) like lower(concat('%', :search, '%'))
        or lower(loan.teacherDniSnapshot) like lower(concat('%', :search, '%'))
        or lower(loan.destinationNameSnapshot) like lower(concat('%', :search, '%'))
        or lower(loanAsset.assetCodeSnapshot) like lower(concat('%', :search, '%'))
        or lower(loanAsset.assetNameSnapshot) like lower(concat('%', :search, '%'))
      )
      order by loan.createdAt desc
      """)
  @EntityGraph(attributePaths = {"teacher", "destinationLocation", "assets", "assets.asset", "assets.asset.assetType", "assets.asset.assetType.category"})
  List<Loan> search(@Param("search") String search);

  @Query("""
      select distinct loan
      from Loan loan
      join loan.assets loanAsset
      where loan.completedAt is null
        and loanAsset.asset.id = :assetId
      """)
  Optional<Loan> findActiveLoanByAssetId(@Param("assetId") UUID assetId);

  @Query("""
      select distinct loan
      from Loan loan
      join loan.assets loanAsset
      where loan.completedAt is null
        and loanAsset.asset.id in :assetIds
      """)
  List<Loan> findActiveLoansByAssetIds(@Param("assetIds") List<UUID> assetIds);
}
