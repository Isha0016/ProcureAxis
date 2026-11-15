package com.spo.sourcing_and_procurement_operations.repositories;

import com.fsc.supply_chain_common_operations.enums.SourcingInvitationStatus;
import com.fsc.supply_chain_common_operations.models.SourcingSupplierInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SourcingSupplierInvitationRepo
        extends JpaRepository<SourcingSupplierInvitation, UUID> {

    List<SourcingSupplierInvitation>
    findBySourcingRequest_SysIdAndStatus(
            UUID sourcingRequestId,
            SourcingInvitationStatus status);

    List<SourcingSupplierInvitation>
    findBySupplierCompany_SysIdAndStatus(
            UUID supplierCompanyId,
            SourcingInvitationStatus status);

    Optional<SourcingSupplierInvitation>
    findBySourcingRequest_SysIdAndSupplierCompany_SysIdAndStatus(
            UUID sourcingRequestId,
            UUID supplierCompanyId,
            SourcingInvitationStatus status);

    void deleteBySourcingRequest_SysId(UUID sourcingRequestId);
}