package com.spo.sourcing_and_procurement_operations.dtos.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PurchaseRequestLineRequest {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0001",
            message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;
}
