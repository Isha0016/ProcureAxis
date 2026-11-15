package com.fsc.supply_chain_common_operations.repositories;

import com.fsc.supply_chain_common_operations.models.SourcingAward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SourcingAwardRepo
        extends JpaRepository<SourcingAward, UUID> {

    Optional<SourcingAward> findBySourcingRequest_SysId(
            UUID sourcingRequestId
    );

    boolean existsBySourcingRequest_SysId(
            UUID sourcingRequestId
    );
}
