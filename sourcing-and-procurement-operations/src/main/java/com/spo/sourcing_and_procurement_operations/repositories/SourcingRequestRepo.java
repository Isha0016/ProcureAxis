package com.spo.sourcing_and_procurement_operations.repositories;

import com.fsc.supply_chain_common_operations.models.SourcingRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SourcingRequestRepo
        extends JpaRepository<SourcingRequest, UUID> {

    Optional<SourcingRequest> findBySysIdAndCompany_SysId(
            UUID sourcingRequestId,
            UUID companyId
    );

    List<SourcingRequest> findByCompany_SysIdOrderByCreatedAtDesc(
            UUID companyId
    );

    boolean existsBySourcePurchaseRequest_SysId(UUID purchaseRequestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sr FROM SourcingRequest sr WHERE sr.sysId = :id")
    Optional<SourcingRequest> findByIdForUpdate(@Param("id") UUID id);
}
