package com.spo.sourcing_and_procurement_operations.dtos.requests;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestDecisionRequest {

    @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
    private String reason;
}