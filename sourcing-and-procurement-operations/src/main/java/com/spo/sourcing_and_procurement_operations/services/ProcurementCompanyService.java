package com.spo.sourcing_and_procurement_operations.services;

import com.fsc.supply_chain_common_operations.enums.CompanyType;
import com.fsc.supply_chain_common_operations.models.Employee;
import com.fsc.supply_chain_common_operations.models.ProcurementCompany;
import com.fsc.supply_chain_common_operations.repositories.CompanyRepo;

import com.fsc.supply_chain_common_operations.services.EmployeeService;
import com.fsc.supply_chain_common_operations.utilities.SystemUtility;
import com.spo.sourcing_and_procurement_operations.dtos.requests.ProcurementCompanyRegistrationRequest;
import com.spo.sourcing_and_procurement_operations.repositories.ProcurementCompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.spo.sourcing_and_procurement_operations.transformer.ProcurementCompanyTransformer;
import com.fsc.supply_chain_common_operations.enums.CompanyStatus;
import com.spo.sourcing_and_procurement_operations.dtos.requests.ProcurementCompanyUpdateRequest;
import com.spo.sourcing_and_procurement_operations.dtos.requests.ProcurementCompanyReviewRequest;
import com.spo.sourcing_and_procurement_operations.dtos.responses.ProcurementCompanyResponse;
import com.spo.sourcing_and_procurement_operations.transformer.ProcurementCompanyResponseTransformer;
import com.spo.sourcing_and_procurement_operations.repositories.ProcurementCompanyRepository;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcurementCompanyService {
    @Autowired
    private ProcurementCompanyRepository procurementCompanyRepository;
    private final CompanyRepo companyRepo;
    private final EmployeeService employeeService;
    private final ProcurementCompanyTransformer transformer;

    public ProcurementCompanyService(
            CompanyRepo companyRepo,
            EmployeeService employeeService,
            ProcurementCompanyTransformer transformer
    ) {
        this.companyRepo = companyRepo;
        this.employeeService = employeeService;
        this.transformer = transformer;
    }

    @Transactional
    public Employee registerProcurementCompany(
            ProcurementCompanyRegistrationRequest request
    ) {

        // 1. Check whether the company email already exists
        if (companyRepo.existsByEmailAddressIgnoreCase(request.getEmailAddress())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Company email already exists"
            );
        }

        if (companyRepo.existsByGstNumberIgnoreCase(request.getGstNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "GST number already exists"
            );
        }

        if (companyRepo.existsByPanNumberIgnoreCase(request.getPanNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "PAN number already exists"
            );
        }

        // 2. Convert request DTO into ProcurementCompany entity
        ProcurementCompany company = transformer.mapToEntity(request);

        // 3. Generate procurement company code
        company.setProcurementCode(
                SystemUtility.generateId("PROC")
        );

        // 4. Create company, main role and main employee
        return employeeService.createMaintUserForCompany(
                company,
                CompanyType.PROCUREMENT_COMPANY
        );
    }
    public Page<ProcurementCompanyResponse> getAllCompanies(
            Pageable pageable) {

        return procurementCompanyRepository.findAll(pageable)
                .map(ProcurementCompanyResponseTransformer::mapToResponse);
    }
    public ProcurementCompanyResponse getCompanyById(UUID id) {

        ProcurementCompany company = procurementCompanyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Procurement company not found"
                ));

        return ProcurementCompanyResponseTransformer.mapToResponse(company);
    }
    @Transactional
    public ProcurementCompanyResponse updateCompany(
            UUID id,
            ProcurementCompanyUpdateRequest request) {

        ProcurementCompany company =  procurementCompanyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Procurement company not found"
                ));

        if (request.getLegalName() != null) {
            company.setLegalName(request.getLegalName());
        }

        if (request.getWebsite() != null) {
            company.setWebsite(request.getWebsite());
        }

        if (request.getContactNumber() != null) {
            company.setContactNumber(request.getContactNumber());
        }

        ProcurementCompany updatedCompany =
                procurementCompanyRepository.save(company);

        return ProcurementCompanyResponseTransformer
                .mapToResponse(updatedCompany);
    }
    @Transactional
    public ProcurementCompanyResponse reviewCompany(
            UUID id,
            ProcurementCompanyReviewRequest request) {

        ProcurementCompany company = procurementCompanyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Procurement company not found"
                ));

        if (company.getStatus() != CompanyStatus.PENDING &&
                company.getStatus() != CompanyStatus.UNDER_REVIEW) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Company is not eligible for review"
            );
        }

        String decision = request.getDecision();

        if ("APPROVE".equalsIgnoreCase(decision)) {

            company.setStatus(CompanyStatus.ACTIVE);
            company.setRejectionReason(null);

        } else if ("REJECT".equalsIgnoreCase(decision)) {

            if (request.getRejectionReason() == null ||
                    request.getRejectionReason().isBlank()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Rejection reason is required"
                );
            }

            company.setStatus(CompanyStatus.REJECTED);
            company.setRejectionReason(request.getRejectionReason());

        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Decision must be APPROVE or REJECT"
            );
        }

        return ProcurementCompanyResponseTransformer.mapToResponse(
                    procurementCompanyRepository.save(company)
        );
    }
    @Transactional
    public ProcurementCompany suspendCompany(UUID id) {

        ProcurementCompany company = procurementCompanyRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Procurement company not found"));

        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only ACTIVE companies can be suspended");
        }

        company.setStatus(CompanyStatus.SUSPENDED);

        return procurementCompanyRepository.save(company);
    }
    @Transactional
    public ProcurementCompany reinstateCompany(UUID id) {

        ProcurementCompany company = procurementCompanyRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Procurement company not found"));

        if (company.getStatus() != CompanyStatus.SUSPENDED &&
                company.getStatus() != CompanyStatus.INACTIVE) {
            throw new IllegalStateException(
                    "Only SUSPENDED or INACTIVE companies can be reinstated");
        }

        company.setStatus(CompanyStatus.UNDER_REVIEW);

        return procurementCompanyRepository.save(company);
    }
}
