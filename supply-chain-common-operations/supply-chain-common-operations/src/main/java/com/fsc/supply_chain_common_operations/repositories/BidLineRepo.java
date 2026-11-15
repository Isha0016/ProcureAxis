package com.fsc.supply_chain_common_operations.repositories;

import com.fsc.supply_chain_common_operations.models.BidLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BidLineRepo extends JpaRepository<BidLine, UUID> {

    List<BidLine> findByBid_SysIdOrderByCreatedAtAsc(UUID bidId);

    void deleteByBid_SysId(UUID bidId);

    boolean existsByBid_SysIdAndSourcingRequestLine_SysId(
            UUID bidId,
            UUID sourcingLineId
    );
}
