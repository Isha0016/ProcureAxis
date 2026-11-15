package com.spo.sourcing_and_procurement_operations.dtos.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class BidLineRequest {

    @NotNull
    private UUID sourcingRequestLineSysId;

    @NotNull
    @DecimalMin(value = "0.0001")
    private BigDecimal quotedQuantity;

    @NotNull
    @DecimalMin(value = "0.0000")
    private BigDecimal quotedUnitPrice;

    @Size(max = 2000)
    private String remarks;
}
