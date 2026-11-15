package com.spo.sourcing_and_procurement_operations.dtos.requests;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class SourcingRequestPublishRequest {

    @NotNull(message = "Response deadline is required")
    @Future(message = "Response deadline must be in the future")
    private Instant responseDeadline;

    @NotNull(message = "Evaluation deadline is required")
    @FutureOrPresent(message = "Evaluation deadline must be in the future")
    private Instant evaluationDeadline;
}
