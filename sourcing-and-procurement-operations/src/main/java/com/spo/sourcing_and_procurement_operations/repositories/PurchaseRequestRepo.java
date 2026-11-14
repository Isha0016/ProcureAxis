package com.spo.sourcing_and_procurement_operations.repositories;

import com.fsc.supply_chain_common_operations.models.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseRequestRepo
        extends JpaRepository<PurchaseRequest, UUID> {

    boolean existsByRequestNumber(String requestNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pr FROM PurchaseRequest pr WHERE pr.sysId = :id")
    Optional<PurchaseRequest> findByIdForUpdate(@Param("id") UUID id);
}