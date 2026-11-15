package com.spo.sourcing_and_procurement_operations.dtos.requests;

import com.fsc.supply_chain_common_operations.enums.SourcingClosureOutcome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SourcingRequestCloseRequest {

    @NotNull
    private SourcingClosureOutcome closureOutcome;

    @NotBlank(message = "Closure reason is required")
    @Size(max = 1000)
    private String reason;
}
