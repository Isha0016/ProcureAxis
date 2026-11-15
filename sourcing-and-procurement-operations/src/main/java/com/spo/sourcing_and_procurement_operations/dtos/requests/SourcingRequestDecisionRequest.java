package com.spo.sourcing_and_procurement_operations.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SourcingRequestDecisionRequest {

    @NotBlank(message = "Decision reason is required")
    @Size(max = 1000)
    private String reason;
}
