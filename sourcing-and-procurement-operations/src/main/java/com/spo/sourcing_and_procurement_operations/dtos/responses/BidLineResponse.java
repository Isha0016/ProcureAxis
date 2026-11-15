package com.spo.sourcing_and_procurement_operations.dtos.responses;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class BidLineResponse {

    private UUID sysId;

    private UUID sourcingRequestLineSysId;

    private Integer sourcingLineNumber;

    private String itemDescription;

    private BigDecimal requiredQuantity;

    private BigDecimal quotedQuantity;

    private BigDecimal quotedUnitPrice;

    private BigDecimal lineAmount;

    private String currencyCode;
}