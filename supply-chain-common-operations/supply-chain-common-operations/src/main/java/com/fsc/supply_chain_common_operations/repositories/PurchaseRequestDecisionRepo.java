package com.fsc.supply_chain_common_operations.repositories;

import com.fsc.supply_chain_common_operations.models.PurchaseRequest;
import com.fsc.supply_chain_common_operations.models.PurchaseRequestDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PurchaseRequestDecisionRepo
        extends JpaRepository<PurchaseRequestDecision, UUID> {

    List<PurchaseRequestDecision>
    findByPurchaseRequestOrderByDecisionAtAsc(
            PurchaseRequest purchaseRequest
    );

    List<PurchaseRequestDecision>
    findByPurchaseRequest_SysIdOrderByDecisionAtAsc(
            UUID purchaseRequestSysId
    );
}