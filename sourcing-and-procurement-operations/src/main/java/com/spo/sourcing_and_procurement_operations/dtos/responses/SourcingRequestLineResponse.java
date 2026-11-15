package com.spo.sourcing_and_procurement_operations.dtos.responses;

import com.fsc.supply_chain_common_operations.enums.UnitOfMeasure;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class SourcingRequestLineResponse {

    private UUID sysId;

    private Integer lineNumber;

    private UUID sourceProductSysId;

    private String productCode;

    private String productCodeSnapshot;

    private String itemDescription;

    private BigDecimal quantity;

    private UnitOfMeasure unitOfMeasure;

    private String notes;

    private BigDecimal estimatedUnitPrice;
    private BigDecimal estimatedLineAmount;
    private String currencyCode;
    private LocalDate requiredDate;
    private String deliveryLocation;
    private BigDecimal taxRate;
}