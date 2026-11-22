package com.slo.supplier_lifecycle_operations.services;

import java.util.UUID;

import com.fsc.supply_chain_common_operations.clients.GeminiClient;
import com.fsc.supply_chain_common_operations.enums.CompanyType;
import com.fsc.supply_chain_common_operations.enums.CompanyStatus;
import com.fsc.supply_chain_common_operations.exceptions.DuplicateRecordException;
import com.fsc.supply_chain_common_operations.exceptions.RecordDoesNotExistException;
import com.fsc.supply_chain_common_operations.models.Employee;
import com.fsc.supply_chain_common_operations.models.SupplierCompany;
import com.fsc.supply_chain_common_operations.services.AuthService;
import com.fsc.supply_chain_common_operations.services.EmployeeService;
import com.fsc.supply_chain_common_operations.services.EmployeeService.MaintUserCreationResult;
import com.fsc.supply_chain_common_operations.repositories.SupplierCompanyRepo;

import com.slo.supplier_lifecycle_operations.dtos.requests.SupplierCompanyRegistrationRequest;
import com.slo.supplier_lifecycle_operations.dtos.requests.SupplierCompanyUpdateRequest;
import com.slo.supplier_lifecycle_operations.transformers.RequestTransformer;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class SupplierCompanyService {

    private final SupplierCompanyRepo supplierCompanyRepo;
    private final EmployeeService employeeService;
    private final AuthService authService;

    @Autowired
    public SupplierCompanyService(
            SupplierCompanyRepo supplierCompanyRepo,
            EmployeeService employeeService,
            AuthService authService
    ) {
        this.supplierCompanyRepo = supplierCompanyRepo;
        this.employeeService = employeeService;
        this.authService = authService;
    }

    // =========================================================
    // SUPPLIER COMPANY REGISTRATION
    // =========================================================

    @Transactional
    public SupplierCompany registerSupplierCompany(
            SupplierCompanyRegistrationRequest supplierCompanyRegistrationRequest
    ) {

        log.info(
                "Starting supplier company registration. companyName={}",
                supplierCompanyRegistrationRequest.getCompanyName()
        );

        // 1. Check duplicate email
        if (supplierCompanyRepo.existsByEmailAddressIgnoreCase(
                supplierCompanyRegistrationRequest.getEmailAddress())) {

            throw new DuplicateRecordException(
                    "A supplier company with this email address already exists."
            );
        }

        // 2. Check duplicate company name
        if (supplierCompanyRepo.existsByCompanyNameIgnoreCase(
                supplierCompanyRegistrationRequest.getCompanyName())) {

            throw new DuplicateRecordException(
                    "A supplier company with this name already exists."
            );
        }

        // 3. Map registration request to SupplierCompany
        SupplierCompany supplierCompany =
                RequestTransformer.mapRegistrationRequestToSupplierCompany(
                        supplierCompanyRegistrationRequest
                );

        supplierCompany.setCompanyType(
                CompanyType.SUPPLIER_COMPANY
        );

        // 4. Generate AI feedback
        String feedback = getSupplierCompanyFeedback(supplierCompany);

        // 5. Save supplier company
        supplierCompany = saveOrUpdate(supplierCompany);
        supplierCompany.setAiFeedback(feedback);
        supplierCompany = saveOrUpdate(supplierCompany);

        log.info(
                "Supplier company saved successfully. companyName={}, companyId={}",
                supplierCompany.getCompanyName(),
                supplierCompany.getSysId()
        );

        // 6. Create main employee and send activation notification.
        // Activation token generation and notification delivery are
        // handled inside Common EmployeeService.
        log.info(
                "Creating main employee for supplier company. companyId={}",
                supplierCompany.getSysId()
        );

        Employee maintEmployee =
                employeeService.createMaintUserForCompany(
                        supplierCompany,
                        CompanyType.SUPPLIER_COMPANY
                );

        log.info(
                "Main employee created successfully. companyId={}, employeeId={}",
                supplierCompany.getSysId(),
                maintEmployee.getSysId()
        );

        log.info(
                "Supplier company registration completed successfully. companyId={}",
                supplierCompany.getSysId()
        );

        // No temporary-password email is sent from SLO.
        // Common EmployeeService already sends the activation notification.

        return supplierCompany;
    }

    // =========================================================
    // SAVE OR UPDATE
    // =========================================================

    public SupplierCompany saveOrUpdate(
            SupplierCompany supplierCompany
    ) {
        return supplierCompanyRepo.save(supplierCompany);
    }

    // =========================================================
    // SUPPLIER COMPANY REVIEW
    // =========================================================

    @Transactional
    public SupplierCompany startSupplierCompanyReview(UUID companyId) {

        SupplierCompany company = supplierCompanyRepo.findById(companyId)
                .orElseThrow(() ->
                        new RecordDoesNotExistException(
                                "Supplier company not found: " + companyId
                        )
                );

        if (company.getStatus() != CompanyStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING suppliers can move to UNDER_REVIEW."
            );
        }

        company.setStatus(CompanyStatus.UNDER_REVIEW);

        return supplierCompanyRepo.save(company);
    }

    // =========================================================
    // APPROVE SUPPLIER COMPANY
    // =========================================================

    @Transactional
    public SupplierCompany approveSupplierCompany(UUID companyId) {

        SupplierCompany company = supplierCompanyRepo.findById(companyId)
                .orElseThrow(() ->
                        new RecordDoesNotExistException(
                                "Supplier company not found: " + companyId
                        )
                );

        if (company.getStatus() != CompanyStatus.UNDER_REVIEW) {
            throw new IllegalStateException(
                    "Only suppliers UNDER_REVIEW can be approved."
            );
        }

        company.setStatus(CompanyStatus.ACTIVE);

        return supplierCompanyRepo.save(company);
    }

    // =========================================================
    // SUSPEND SUPPLIER COMPANY
    // =========================================================

    @Transactional
    public SupplierCompany suspendSupplierCompany(UUID companyId) {

        SupplierCompany company = supplierCompanyRepo.findById(companyId)
                .orElseThrow(() ->
                        new RecordDoesNotExistException(
                                "Supplier company not found: " + companyId
                        )
                );

        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only ACTIVE suppliers can be suspended."
            );
        }

        company.setStatus(CompanyStatus.SUSPENDED);

        return supplierCompanyRepo.save(company);
    }

    // =========================================================
    // DEACTIVATE SUPPLIER COMPANY
    // =========================================================

    @Transactional
    public SupplierCompany deactivateSupplierCompany(UUID companyId) {

        SupplierCompany company = supplierCompanyRepo.findById(companyId)
                .orElseThrow(() ->
                        new RecordDoesNotExistException(
                                "Supplier company not found: " + companyId
                        )
                );

        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only ACTIVE suppliers can be deactivated."
            );
        }

        company.setStatus(CompanyStatus.INACTIVE);

        return supplierCompanyRepo.save(company);
    }

    // =========================================================
    // REINSTATE SUPPLIER COMPANY
    // =========================================================

    @Transactional
    public SupplierCompany reinstateSupplierCompany(UUID companyId) {

        SupplierCompany company = supplierCompanyRepo.findById(companyId)
                .orElseThrow(() ->
                        new RecordDoesNotExistException(
                                "Supplier company not found: " + companyId
                        )
                );

        if (company.getStatus() != CompanyStatus.SUSPENDED
                && company.getStatus() != CompanyStatus.INACTIVE) {

            throw new IllegalStateException(
                    "Only SUSPENDED or INACTIVE suppliers can be reinstated."
            );
        }

        // Reinstatement requires another internal review.
        company.setStatus(CompanyStatus.UNDER_REVIEW);

        return supplierCompanyRepo.save(company);
    }

    // =========================================================
    // REJECT SUPPLIER COMPANY
    // =========================================================

    @Transactional
    public SupplierCompany rejectSupplierCompany(
            UUID companyId,
            String rejectionReason
    ) {

        SupplierCompany company = supplierCompanyRepo.findById(companyId)
                .orElseThrow(() ->
                        new RecordDoesNotExistException(
                                "Supplier company not found: " + companyId
                        )
                );

        if (company.getStatus() != CompanyStatus.UNDER_REVIEW) {
            throw new IllegalStateException(
                    "Only suppliers UNDER_REVIEW can be rejected."
            );
        }

        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new IllegalArgumentException(
                    "Rejection reason is required."
            );
        }

        company.setStatus(CompanyStatus.REJECTED);
        company.setRejectionReason(rejectionReason.trim());

        return supplierCompanyRepo.save(company);
    }

    // =========================================================
    // GET SUPPLIER COMPANY
    // =========================================================

    @Transactional(readOnly = true)
    public SupplierCompany getSupplierCompanyById(
            UUID companyId,
            String token
    ) {

        Claims claims = (Claims) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String authenticatedCompanyId =
                claims.get("companyId", String.class);

        // Supplier can access its own company.
        if (companyId.toString().equals(authenticatedCompanyId)) {

            return supplierCompanyRepo.findById(companyId)
                    .orElseThrow(() ->
                            new RecordDoesNotExistException(
                                    "Supplier company not found: " + companyId
                            )
                    );
        }

        // Access to another company requires reviewer permission.
        authService.isUserHavingAccess(
                token,
                "APPROVE_SUPPLIER"
        );

        return supplierCompanyRepo.findById(companyId)
                .orElseThrow(() ->
                        new RecordDoesNotExistException(
                                "Supplier company not found: " + companyId
                        )
                );
    }

    // =========================================================
    // UPDATE SUPPLIER COMPANY
    // =========================================================

    @Transactional
    public SupplierCompany updateSupplierCompany(
            UUID companyId,
            SupplierCompanyUpdateRequest request
    ) {

        Claims claims = (Claims) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String authenticatedCompanyId =
                claims.get("companyId", String.class);

        if (!companyId.toString().equals(authenticatedCompanyId)) {
            throw new AccessDeniedException(
                    "You are not authorized to update this company"
            );
        }

        SupplierCompany company = supplierCompanyRepo.findById(companyId)
                .orElseThrow(() ->
                        new RecordDoesNotExistException(
                                "Supplier company not found: " + companyId
                        )
                );

        // Update common company fields
        if (request.getCompanyName() != null) {
            company.setCompanyName(request.getCompanyName());
        }

        if (request.getLegalName() != null) {
            company.setLegalName(request.getLegalName());
        }

        if (request.getGstNumber() != null) {
            company.setGstNumber(request.getGstNumber());
        }

        if (request.getPanNumber() != null) {
            company.setPanNumber(request.getPanNumber());
        }

        if (request.getWebsite() != null) {
            company.setWebsite(request.getWebsite());
        }

        if (request.getContactNumber() != null) {
            company.setContactNumber(request.getContactNumber());
        }

        // Update supplier-specific fields
        if (request.getSupplierCategory() != null) {
            company.setSupplierCategory(request.getSupplierCategory());
        }

        if (request.getLeadTimeDays() != null) {
            company.setLeadTimeDays(request.getLeadTimeDays());
        }

        if (request.getMinimumOrderValue() != null) {
            company.setMinimumOrderValue(request.getMinimumOrderValue());
        }

        return supplierCompanyRepo.save(company);
    }

    // =========================================================
    // SUPPLIER COMPANY AI FEEDBACK
    // =========================================================

    public String getSupplierCompanyFeedback(
            SupplierCompany supplierCompany
    ) {

        String prompt =
                "Analyze the following supplier company details and provide comprehensive, "
                        + "constructive feedback about the supplier's overall performance, strengths, "
                        + "weaknesses, risks, and areas for improvement.\n\n"
                        + "Supplier Company Payload:\n"
                        + supplierCompany.toString();

        GeminiClient geminiClient = new GeminiClient();

        return geminiClient.getAIResponse(prompt);
    }
}