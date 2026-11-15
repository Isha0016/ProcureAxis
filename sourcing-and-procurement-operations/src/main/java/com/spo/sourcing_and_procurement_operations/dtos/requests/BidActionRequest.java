package com.spo.sourcing_and_procurement_operations.dtos.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BidActionRequest {

    @NotNull
    private Long expectedVersion;
}