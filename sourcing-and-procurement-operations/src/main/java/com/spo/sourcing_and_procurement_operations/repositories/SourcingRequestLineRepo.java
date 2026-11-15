package com.spo.sourcing_and_procurement_operations.repositories;

import com.fsc.supply_chain_common_operations.models.SourcingRequestLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SourcingRequestLineRepo
        extends JpaRepository<SourcingRequestLine, UUID> {

    List<SourcingRequestLine> findBySourcingRequest_SysIdOrderByLineNumberAsc(
            UUID sourcingRequestId
    );

    void deleteBySourcingRequest_SysId(UUID sourcingRequestId);
}
