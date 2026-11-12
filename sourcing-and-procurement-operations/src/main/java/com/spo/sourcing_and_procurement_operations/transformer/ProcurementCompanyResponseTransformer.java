package com.spo.sourcing_and_procurement_operations.transformer;

import com.fsc.supply_chain_common_operations.models.ProcurementCompany;
import com.spo.sourcing_and_procurement_operations.dtos.responses.ProcurementCompanyResponse;

public class ProcurementCompanyResponseTransformer {

    public static ProcurementCompanyResponse mapToResponse(
            ProcurementCompany company) {

        ProcurementCompanyResponse response =
                new ProcurementCompanyResponse();

        response.setId(company.getSysId());
        response.setCompanyCode(company.getCompanyCode());
        response.setCompanyName(company.getCompanyName());
        response.setLegalName(company.getLegalName());
        response.setGstNumber(company.getGstNumber());
        response.setPanNumber(company.getPanNumber());
        response.setEmailAddress(company.getEmailAddress());
        response.setWebsite(company.getWebsite());
        response.setContactNumber(company.getContactNumber());
        response.setProcurementCode(company.getProcurementCode());
        response.setFiscalYearStartMonth(company.getFiscalYearStartMonth());
        response.setApprovalRequired(company.getApprovalRequired());
        response.setStatus(company.getStatus());
        response.setRejectionReason(company.getRejectionReason());

        return response;
    }
}