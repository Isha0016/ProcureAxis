package com.fsc.supply_chain_common_operations.repositories;

import com.fsc.supply_chain_common_operations.models.SourcingRequest;
import com.fsc.supply_chain_common_operations.models.SourcingRequestDecision;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SourcingRequestDecisionRepo
        extends JpaRepository<SourcingRequestDecision, UUID> {

    List<SourcingRequestDecision>
    findBySourcingRequestOrderByDecisionAtAsc(
            SourcingRequest sourcingRequest
    );

    List<SourcingRequestDecision>
    findBySourcingRequest_SysIdOrderByDecisionAtAsc(
            UUID sourcingRequestSysId
    );
}