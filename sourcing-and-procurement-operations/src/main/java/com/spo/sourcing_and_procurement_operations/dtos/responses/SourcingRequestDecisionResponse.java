package com.spo.sourcing_and_procurement_operations.dtos.responses;

import com.fsc.supply_chain_common_operations.enums.RequestStatus;
import com.fsc.supply_chain_common_operations.enums.SourcingRequestDecisionType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SourcingRequestDecisionResponse {

    private UUID sysId;

    private UUID actorEmployeeSysId;

    private String actorName;

    private SourcingRequestDecisionType decisionType;

    private RequestStatus fromStatus;

    private RequestStatus toStatus;

    private String reason;

    private LocalDateTime decisionAt;
}
