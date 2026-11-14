package com.spo.sourcing_and_procurement_operations.dtos.responses;

import com.fsc.supply_chain_common_operations.enums.PurchaseRequestDecisionType;
import com.fsc.supply_chain_common_operations.enums.RequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PurchaseRequestDecisionResponse {

    private UUID sysId;

    private UUID actorEmployeeSysId;

    private String actorName;

    private PurchaseRequestDecisionType decisionType;

    private RequestStatus fromStatus;

    private RequestStatus toStatus;

    private String reason;

    private LocalDateTime decisionAt;
}