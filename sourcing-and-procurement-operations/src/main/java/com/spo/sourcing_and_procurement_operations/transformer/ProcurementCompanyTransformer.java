package com.spo.sourcing_and_procurement_operations.transformer;

import com.fsc.supply_chain_common_operations.enums.CompanyStatus;
import com.fsc.supply_chain_common_operations.enums.CompanyType;
import com.fsc.supply_chain_common_operations.models.ProcurementCompany;
import com.spo.sourcing_and_procurement_operations.dtos.requests.ProcurementCompanyRegistrationRequest;
import com.spo.sourcing_and_procurement_operations.dtos.responses.ProcurementCompanyResponse;
import org.springframework.stereotype.Component;

@Component
public class ProcurementCompanyTransformer {

    public ProcurementCompany mapToEntity(
            ProcurementCompanyRegistrationRequest request) {

        ProcurementCompany company = new ProcurementCompany();

        // Common company details
        company.setCompanyName(request.getCompanyName());
        company.setLegalName(request.getLegalName());
        company.setGstNumber(request.getGstNumber());
        company.setPanNumber(request.getPanNumber());
        company.setEmailAddress(request.getEmailAddress());
        company.setWebsite(request.getWebsite());
        company.setContactNumber(request.getContactNumber());

        // Company classification
        company.setCompanyType(CompanyType.PROCUREMENT_COMPANY);
        company.setStatus(CompanyStatus.PENDING);

        // Procurement-specific details
        company.setFiscalYearStartMonth(
                request.getFiscalYearStartMonth());

        company.setApprovalRequired(
                request.getApprovalRequired());

        return company;
    }
    public static ProcurementCompanyResponse mapToResponse(

            ProcurementCompany company) {

        ProcurementCompanyResponse response =

                new ProcurementCompanyResponse();

        response.setId(company.getSysId());

        response.setCompanyName(company.getCompanyName());

        response.setLegalName(company.getLegalName());

        response.setGstNumber(company.getGstNumber());

        response.setPanNumber(company.getPanNumber());

        response.setEmailAddress(company.getEmailAddress());

        response.setWebsite(company.getWebsite());

        response.setContactNumber(company.getContactNumber());

        response.setStatus(company.getStatus());

        return response;

    }
}
