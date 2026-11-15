package com.spo.sourcing_and_procurement_operations.dtos.responses;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SourcingAwardResponse {

    private UUID sysId;

    private UUID sourcingRequestSysId;

    private UUID acceptedBidSysId;

    private String acceptedBidNumber;

    private UUID awardedByEmployeeSysId;

    private String awardedByName;

    private String reason;

    private Instant awardedAt;
}