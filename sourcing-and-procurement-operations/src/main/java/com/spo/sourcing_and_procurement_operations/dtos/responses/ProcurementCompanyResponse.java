package com.spo.sourcing_and_procurement_operations.dtos.responses;

import com.fsc.supply_chain_common_operations.enums.CompanyStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class ProcurementCompanyResponse {

    private UUID id;
    private String companyCode;
    private String companyName;
    private String legalName;
    private String gstNumber;
    private String panNumber;
    private String emailAddress;
    private String website;
    private String contactNumber;
    private String procurementCode;
    private Integer fiscalYearStartMonth;
    private Boolean approvalRequired;
    private CompanyStatus status;
    private String rejectionReason;

}