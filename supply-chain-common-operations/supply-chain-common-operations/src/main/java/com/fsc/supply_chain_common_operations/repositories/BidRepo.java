package com.fsc.supply_chain_common_operations.repositories;

import com.fsc.supply_chain_common_operations.models.Bid;
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
public interface BidRepo extends JpaRepository<Bid, UUID> {

    Optional<Bid> findBySourcingRequest_SysIdAndSupplierCompany_SysId(
            UUID sourcingRequestId,
            UUID supplierCompanyId
    );

    boolean existsBySourcingRequest_SysIdAndSupplierCompany_SysId(
            UUID sourcingRequestId,
            UUID supplierCompanyId
    );

    List<Bid> findBySourcingRequest_SysIdOrderByCreatedAtAsc(
            UUID sourcingRequestId
    );

    List<Bid> findBySupplierCompany_SysIdOrderByCreatedAtDesc(
            UUID supplierCompanyId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Bid b WHERE b.sysId = :id")
    Optional<Bid> findByIdForUpdate(@Param("id") UUID id);
}