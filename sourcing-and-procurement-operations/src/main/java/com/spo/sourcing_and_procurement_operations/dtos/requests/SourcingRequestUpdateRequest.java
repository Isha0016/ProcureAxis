package com.spo.sourcing_and_procurement_operations.dtos.requests;

import com.fsc.supply_chain_common_operations.enums.SourcingMethod;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class SourcingRequestUpdateRequest {

    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    private SourcingMethod sourcingMethod;

    @Size(max = 2000)
    private String evaluationCriteria;

    @Size(max = 2000)
    private String commercialTerms;

    private Instant responseDeadline;

    private Instant evaluationDeadline;

    private List<UUID> supplierCompanyIds;
}