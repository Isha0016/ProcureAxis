package com.spo.sourcing_and_procurement_operations.dtos.responses;

import com.fsc.supply_chain_common_operations.enums.SourcingInvitationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SourcingSupplierInvitationResponse {

    private UUID sysId;

    private UUID supplierCompanySysId;

    private String supplierCompanyName;

    private SourcingInvitationStatus status;

    private Instant invitedAt;
}
