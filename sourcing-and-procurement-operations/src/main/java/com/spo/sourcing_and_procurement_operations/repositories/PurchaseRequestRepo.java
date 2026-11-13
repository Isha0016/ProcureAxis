package com.spo.sourcing_and_procurement_operations.repositories;

import com.fsc.supply_chain_common_operations.models.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PurchaseRequestRepo
        extends JpaRepository<PurchaseRequest, UUID> {

    boolean existsByRequestNumber(String requestNumber);
}