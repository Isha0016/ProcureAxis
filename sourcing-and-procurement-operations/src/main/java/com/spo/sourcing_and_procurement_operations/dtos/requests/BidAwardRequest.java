package com.spo.sourcing_and_procurement_operations.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class BidAwardRequest {

    @NotNull
    private UUID bidSysId;

    @NotNull
    private Long expectedSourcingVersion;

    @NotBlank
    @Size(max = 2000)
    private String reason;
}