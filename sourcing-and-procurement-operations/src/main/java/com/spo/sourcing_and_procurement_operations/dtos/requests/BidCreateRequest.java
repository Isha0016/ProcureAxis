package com.spo.sourcing_and_procurement_operations.dtos.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BidCreateRequest {

    private Long expectedVersion;

    @NotNull
    private LocalDate validUntil;

    @NotNull
    @Min(1)
    @Max(3650)
    private Integer deliveryLeadTimeDays;

    @Size(max = 1000)
    private String paymentTerms;

    @Size(max = 1000)
    private String deliveryTerms;

    private String technicalProposal;

    private String commercialProposal;

    @Size(max = 2000)
    private String remarks;

    @NotEmpty
    @Valid
    private List<BidLineRequest> lines;
}