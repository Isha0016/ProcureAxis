package com.spo.sourcing_and_procurement_operations.dtos.requests;

import lombok.Data;

@Data
public class ProcurementCompanyReviewRequest {

    private String decision;
    private String rejectionReason;
}