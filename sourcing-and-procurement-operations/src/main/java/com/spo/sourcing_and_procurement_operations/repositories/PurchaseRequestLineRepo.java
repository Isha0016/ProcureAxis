package com.spo.sourcing_and_procurement_operations.repositories;

import com.fsc.supply_chain_common_operations.models.PurchaseRequestLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PurchaseRequestLineRepo
        extends JpaRepository<PurchaseRequestLine, UUID> {
    List<PurchaseRequestLine> findByPurchaseRequest_SysId(UUID purchaseRequestId);
}