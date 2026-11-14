package com.spo.sourcing_and_procurement_operations.dtos.responses;

import com.fsc.supply_chain_common_operations.enums.UnitOfMeasure;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class PurchaseRequestLineResponse {

    private UUID sysId;

    private Integer lineNumber;

    private UUID productId;

    private String productCode;

    private String itemDescription;

    private BigDecimal quantity;

    private UnitOfMeasure unitOfMeasure;

    private BigDecimal estimatedUnitPrice;

    private BigDecimal estimatedLineAmount;

    private String currencyCode;

    private String notes;
}