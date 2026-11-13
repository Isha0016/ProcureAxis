package com.slo.supplier_lifecycle_operations.dtos.requests;

import com.fsc.supply_chain_common_operations.enums.ProductStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductStatusUpdateRequest {

    @NotNull(message = "Product status is required")
    private ProductStatus status;
}