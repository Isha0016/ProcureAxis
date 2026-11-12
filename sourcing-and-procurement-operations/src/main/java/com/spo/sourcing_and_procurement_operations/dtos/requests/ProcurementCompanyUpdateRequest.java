package com.spo.sourcing_and_procurement_operations.dtos.requests;

import lombok.Data;

@Data
public class ProcurementCompanyUpdateRequest {

    private String legalName;
    private String website;
    private String contactNumber;
}
