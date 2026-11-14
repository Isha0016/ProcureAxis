package com.spo.sourcing_and_procurement_operations.dtos.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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
    @Digits(integer = 15, fraction = 4)
    private BigDecimal quantity;

    @NotNull(message = "Estimated unit price is required")
    @DecimalMin(value = "0.0000",
            message = "Estimated unit price cannot be negative")
    @Digits(integer = 15, fraction = 4)
    private BigDecimal estimatedUnitPrice;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;
}
