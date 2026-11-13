package com.spo.sourcing_and_procurement_operations.services;

import com.fsc.supply_chain_common_operations.enums.CompanyType;
import com.fsc.supply_chain_common_operations.models.Employee;
import com.fsc.supply_chain_common_operations.models.ProcurementCompany;
import com.fsc.supply_chain_common_operations.repositories.CompanyRepo;

import com.fsc.supply_chain_common_operations.services.EmployeeService;
import com.fsc.supply_chain_common_operations.utilities.SystemUtility;
import com.spo.sourcing_and_procurement_operations.dtos.requests.ProcurementCompanyRegistrationRequest;
import com.fsc.supply_chain_common_operations.repositories.ProcurementCompanyRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.spo.sourcing_and_procurement_operations.transformer.ProcurementCompanyTransformer;
import com.fsc.supply_chain_common_operations.enums.CompanyStatus;
import com.spo.sourcing_and_procurement_operations.dtos.requests.ProcurementCompanyUpdateRequest;
import com.spo.sourcing_and_procurement_operations.dtos.requests.ProcurementCompanyReviewRequest;
import com.spo.sourcing_and_procurement_operations.dtos.responses.ProcurementCompanyResponse;
import com.spo.sourcing_and_procurement_operations.transformer.ProcurementCompanyResponseTransformer;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.fsc.supply_chain_common_operations.services.AuthService;
import com.fsc.supply_chain_common_operations.utilities.JwtUtility;
import io.jsonwebtoken.Claims;
import org.springframework.security.access.AccessDeniedException;


import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcurementCompanyService {
    @Autowired
    private ProcurementCompanyRepo procurementCompanyRepo;
    private final CompanyRepo companyRepo;
    private final EmployeeService employeeService;
    private final ProcurementCompanyTransformer transformer;
    private final AuthService authService;
    private final JwtUtility jwtUtility;

    public ProcurementCompanyService(
            CompanyRepo companyRepo,
            EmployeeService employeeService,
            ProcurementCompanyTransformer transformer,
            AuthService authService,
            JwtUtility jwtUtility
    ) {
        this.companyRepo = companyRepo;
        this.employeeService = employeeService;
        this.transformer = transformer;
        this.authService=   authService;
        this.jwtUtility=jwtUtility;
    }

    private void validateCompanyAccess(UUID companyId, String token) {

        Claims claims = jwtUtility.extractAllClaims(token);

        String authenticatedCompanyId =
                claims.get("companyId", String.class);

        if (authenticatedCompanyId == null) {
            throw new AccessDeniedException("Company access denied.");
        }

        // Check whether the requested company belongs to the logged-in employee.
        if (companyId.toString().equals(authenticatedCompanyId)) {

            // Verify that the employee account is still active.
            String email = claims.get("email", String.class);
            Employee employee = employeeService.getEmployeeByEmail(email);

            if (employee == null ||
                    employee.getUserStatus() !=
                            com.fsc.supply_chain_common_operations.enums.UserStatus.ACTIVE ||
                    employee.getCompany() == null ||
                    !companyId.equals(employee.getCompany().getSysId())) {

                throw new AccessDeniedException("Company access denied.");
            }

            return;
        }

        // A different company can only be accessed by an authorized reviewer.
        authService.isUserHavingAccess(token, "APPROVE_PROCUREMENT");
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

        return procurementCompanyRepo.findAll(pageable)
                .map(ProcurementCompanyResponseTransformer::mapToResponse);
    }
    public ProcurementCompanyResponse getCompanyById(
            UUID id,
            String token) {

        validateCompanyAccess(id, token);

        ProcurementCompany company =
                procurementCompanyRepo.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Procurement company not found"
                        ));

        return ProcurementCompanyResponseTransformer.mapToResponse(company);
    }
    @Transactional
    public ProcurementCompanyResponse updateCompany(
            UUID id,
            ProcurementCompanyUpdateRequest request,
            String token) {

        validateCompanyAccess(id, token);

        ProcurementCompany company =
                procurementCompanyRepo.findById(id)
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
                procurementCompanyRepo.save(company);

        return ProcurementCompanyResponseTransformer
                .mapToResponse(updatedCompany);
    }
    @Transactional
    public ProcurementCompanyResponse reviewCompany(
            UUID id,
            ProcurementCompanyReviewRequest request) {

        ProcurementCompany company = procurementCompanyRepo.findById(id)
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
                    procurementCompanyRepo.save(company)
        );
    }
    @Transactional
    public ProcurementCompany suspendCompany(UUID id) {

        ProcurementCompany company = procurementCompanyRepo.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Procurement company not found"));

        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only ACTIVE companies can be suspended");
        }

        company.setStatus(CompanyStatus.SUSPENDED);

        return procurementCompanyRepo.save(company);
    }
    @Transactional
    public ProcurementCompany reinstateCompany(UUID id) {

        ProcurementCompany company = procurementCompanyRepo.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Procurement company not found"));

        if (company.getStatus() != CompanyStatus.SUSPENDED &&
                company.getStatus() != CompanyStatus.INACTIVE) {
            throw new IllegalStateException(
                    "Only SUSPENDED or INACTIVE companies can be reinstated");
        }

        company.setStatus(CompanyStatus.UNDER_REVIEW);

        return procurementCompanyRepo.save(company);
    }
}
