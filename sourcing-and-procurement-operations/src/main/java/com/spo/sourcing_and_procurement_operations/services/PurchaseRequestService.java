package com.spo.sourcing_and_procurement_operations.services;

import com.fsc.supply_chain_common_operations.enums.CompanyStatus;
import com.fsc.supply_chain_common_operations.enums.CompanyType;
import com.fsc.supply_chain_common_operations.enums.EmploymentStatus;
import com.fsc.supply_chain_common_operations.enums.ProductStatus;
import com.fsc.supply_chain_common_operations.enums.PurchaseRequestDecisionType;
import com.fsc.supply_chain_common_operations.enums.RequestLineStatus;
import com.fsc.supply_chain_common_operations.enums.RequestStatus;
import com.fsc.supply_chain_common_operations.enums.UserStatus;
import com.fsc.supply_chain_common_operations.models.Company;
import com.fsc.supply_chain_common_operations.models.Employee;
import com.fsc.supply_chain_common_operations.models.Operation;
import com.fsc.supply_chain_common_operations.models.Product;
import com.fsc.supply_chain_common_operations.models.PurchaseRequest;
import com.fsc.supply_chain_common_operations.models.PurchaseRequestDecision;
import com.fsc.supply_chain_common_operations.models.PurchaseRequestLine;
import com.fsc.supply_chain_common_operations.repositories.EmployeeRepo;
import com.fsc.supply_chain_common_operations.repositories.ProductRepo;
import com.fsc.supply_chain_common_operations.repositories.PurchaseRequestDecisionRepo;
import com.spo.sourcing_and_procurement_operations.dtos.requests.PurchaseRequestCreateRequest;
import com.spo.sourcing_and_procurement_operations.dtos.requests.PurchaseRequestLineRequest;
import com.spo.sourcing_and_procurement_operations.repositories.PurchaseRequestLineRepo;
import com.spo.sourcing_and_procurement_operations.repositories.PurchaseRequestRepo;
import com.spo.sourcing_and_procurement_operations.dtos.requests.PurchaseRequestUpdateRequest;
import io.jsonwebtoken.Claims;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PurchaseRequestService {

    private static final String APPROVE_PURCHASE_REQUEST =
            "APPROVE_PURCHASE_REQUEST";

    private final EmployeeRepo employeeRepo;
    private final ProductRepo productRepo;
    private final PurchaseRequestRepo purchaseRequestRepo;
    private final PurchaseRequestLineRepo purchaseRequestLineRepo;
    private final PurchaseRequestDecisionRepo decisionRepo;

    public PurchaseRequestService(
            EmployeeRepo employeeRepo,
            ProductRepo productRepo,
            PurchaseRequestRepo purchaseRequestRepo,
            PurchaseRequestLineRepo purchaseRequestLineRepo,
            PurchaseRequestDecisionRepo decisionRepo) {

        this.employeeRepo = employeeRepo;
        this.productRepo = productRepo;
        this.purchaseRequestRepo = purchaseRequestRepo;
        this.purchaseRequestLineRepo = purchaseRequestLineRepo;
        this.decisionRepo = decisionRepo;
    }

    // ---------------------------------------------------------
    // CREATE PURCHASE REQUEST - DRAFT
    // ---------------------------------------------------------

    @Transactional
    public PurchaseRequest createPurchaseRequest(
            PurchaseRequestCreateRequest request) {

        Employee employee = getAuthenticatedEmployee();
        Company company = employee.getCompany();

        if (company == null ||
                company.getCompanyType() != CompanyType.PROCUREMENT_COMPANY ||
                company.getStatus() != CompanyStatus.ACTIVE) {

            throw new AccessDeniedException(
                    "Only employees of an active procurement company can create purchase requests");
        }

        PurchaseRequest purchaseRequest = new PurchaseRequest();

        purchaseRequest.setRequestNumber(
                "PR-" + UUID.randomUUID().toString().toUpperCase());

        purchaseRequest.setTitle(request.getTitle().trim());
        purchaseRequest.setDescription(request.getDescription());
        purchaseRequest.setCompany(company);
        purchaseRequest.setSubmitterEmployee(employee);
        purchaseRequest.setRequestStatus(RequestStatus.DRAFT);
        purchaseRequest.setPriority(request.getPriority());
        purchaseRequest.setRequiredDate(request.getRequiredDate());
        purchaseRequest.setDeliveryLocation(request.getDeliveryLocation());
        purchaseRequest.setCurrencyCode(request.getCurrencyCode());
        purchaseRequest.setBudgetCode(request.getBudgetCode());
        purchaseRequest.setPurchaseRequestType(
                request.getPurchaseRequestType());
        purchaseRequest.setProcurementType(request.getProcurementType());

        PurchaseRequest savedRequest =
                purchaseRequestRepo.save(purchaseRequest);

        List<PurchaseRequestLine> requestLines = new ArrayList<>();
        Set<UUID> addedProductIds = new HashSet<>();

        BigDecimal totalAmount = BigDecimal.ZERO;
        int lineNumber = 1;

        for (PurchaseRequestLineRequest lineRequest :
                request.getRequestLines()) {

            Product product = productRepo.findById(lineRequest.getProductId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Product does not exist: "
                                            + lineRequest.getProductId()));

            if (!addedProductIds.add(product.getSysId())) {
                throw new IllegalArgumentException(
                        "Duplicate product in purchase request: "
                                + product.getProductCode());
            }

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new IllegalArgumentException(
                        "Product is not active: " + product.getProductCode());
            }

            if (product.getDescription() == null ||
                    product.getDescription().isBlank()) {
                throw new IllegalArgumentException(
                        "Product description is required for historical snapshot: "
                                + product.getProductCode());
            }

            if (product.getSupplierCompany() == null ||
                    product.getSupplierCompany().getStatus()
                            != CompanyStatus.ACTIVE) {
                throw new IllegalArgumentException(
                        "Product supplier is not active: "
                                + product.getProductCode());
            }

            if (lineRequest.getQuantity() == null ||
                    lineRequest.getQuantity().signum() <= 0) {
                throw new IllegalArgumentException(
                        "Purchase request quantity must be greater than zero");
            }

            if (lineRequest.getEstimatedUnitPrice() == null ||
                    lineRequest.getEstimatedUnitPrice().signum() < 0) {
                throw new IllegalArgumentException(
                        "Estimated unit price cannot be negative");
            }

            BigDecimal lineAmount =
                    lineRequest.getQuantity()
                            .multiply(lineRequest.getEstimatedUnitPrice())
                            .setScale(4, RoundingMode.HALF_UP);

            PurchaseRequestLine requestLine =
                    new PurchaseRequestLine();

            requestLine.setPurchaseRequest(savedRequest);
            requestLine.setLineNumber(lineNumber++);
            requestLine.setProduct(product);
            requestLine.setQuantity(lineRequest.getQuantity());
            requestLine.setNotes(lineRequest.getNotes());
            requestLine.setLineStatus(RequestLineStatus.OPEN);

            // Preserve historical product details.
            requestLine.setItemDescription(product.getDescription());
            requestLine.setUnitOfMeasure(product.getUnitOfMeasure());

            requestLine.setEstimatedUnitPrice(
                    lineRequest.getEstimatedUnitPrice());
            requestLine.setEstimatedLineAmount(lineAmount);
            requestLine.setCurrencyCode(request.getCurrencyCode());

            requestLines.add(requestLine);
            totalAmount = totalAmount.add(lineAmount);
        }

        purchaseRequestLineRepo.saveAll(requestLines);

        savedRequest.setEstimatedTotalAmount(
                totalAmount.setScale(4, RoundingMode.HALF_UP));

        return purchaseRequestRepo.save(savedRequest);
    }

    // ---------------------------------------------------------
    // GET PURCHASE REQUEST
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    public PurchaseRequest getPurchaseRequest(UUID purchaseRequestId) {

        Employee actor = getAuthenticatedEmployee();

        PurchaseRequest purchaseRequest =
                purchaseRequestRepo.findById(purchaseRequestId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Purchase request not found"));

        validateSameCompany(purchaseRequest, actor);

        return purchaseRequest;
    }

    // ---------------------------------------------------------
    // SUBMIT PURCHASE REQUEST
    // DRAFT -> SUBMITTED -> PENDING_APPROVAL
    // ---------------------------------------------------------

    @Transactional
    public PurchaseRequest submitPurchaseRequest(UUID purchaseRequestId) {

        Employee submitter = getAuthenticatedEmployee();

        PurchaseRequest purchaseRequest =
                getPurchaseRequestForUpdate(purchaseRequestId);

        validateSameCompany(purchaseRequest, submitter);
        validateSubmitter(purchaseRequest, submitter);

        if (purchaseRequest.getRequestStatus() != RequestStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT purchase requests can be submitted");
        }

        if (purchaseRequest.getTitle() == null ||
                purchaseRequest.getTitle().isBlank()) {
            throw new IllegalArgumentException("Request title is required");
        }

        if (purchaseRequest.getRequiredDate() == null) {
            throw new IllegalArgumentException("Required date is mandatory");
        }

        if (purchaseRequest.getRequiredDate().isBefore(
                java.time.LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Required date cannot be in the past");
        }

        if (purchaseRequest.getDeliveryLocation() == null ||
                purchaseRequest.getDeliveryLocation().isBlank()) {
            throw new IllegalArgumentException(
                    "Delivery location is mandatory");
        }

        String currencyCode = purchaseRequest.getCurrencyCode();

        if (currencyCode == null ||
                !currencyCode.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException(
                    "A valid three-letter currency code is required");
        }

        List<PurchaseRequestLine> lines =
                purchaseRequestLineRepo.findByPurchaseRequest_SysId(
                        purchaseRequestId);

        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one purchase request line is required");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PurchaseRequestLine line : lines) {

            if (line.getQuantity() == null ||
                    line.getQuantity().signum() <= 0) {
                throw new IllegalArgumentException(
                        "Every line must have a quantity greater than zero");
            }

            if (line.getEstimatedUnitPrice() == null ||
                    line.getEstimatedUnitPrice().signum() < 0) {
                throw new IllegalArgumentException(
                        "Every line must have a nonnegative estimated unit price");
            }

            if (!currencyCode.equals(line.getCurrencyCode())) {
                throw new IllegalArgumentException(
                        "All purchase request lines must use the request currency");
            }

            BigDecimal lineAmount =
                    line.getQuantity()
                            .multiply(line.getEstimatedUnitPrice())
                            .setScale(4, RoundingMode.HALF_UP);

            line.setEstimatedLineAmount(lineAmount);
            totalAmount = totalAmount.add(lineAmount);
        }

        purchaseRequestLineRepo.saveAll(lines);

        purchaseRequest.setEstimatedTotalAmount(
                totalAmount.setScale(4, RoundingMode.HALF_UP));

        Employee manager = submitter.getManager();

        validateApprovalManager(manager, submitter);

        purchaseRequest.setProcurementApprovalManager(manager);
        purchaseRequest.setSubmissionDate(Instant.now());

        // Record both transitions in the same transaction.
        recordDecision(
                purchaseRequest,
                submitter,
                PurchaseRequestDecisionType.SUBMITTED,
                RequestStatus.DRAFT,
                RequestStatus.SUBMITTED,
                null
        );

        purchaseRequest.setRequestStatus(RequestStatus.SUBMITTED);

        recordDecision(
                purchaseRequest,
                submitter,
                PurchaseRequestDecisionType.PENDING_APPROVAL_ASSIGNED,
                RequestStatus.SUBMITTED,
                RequestStatus.PENDING_APPROVAL,
                null
        );

        purchaseRequest.setRequestStatus(RequestStatus.PENDING_APPROVAL);

        return purchaseRequestRepo.save(purchaseRequest);
    }

    // ---------------------------------------------------------
    // APPROVE PURCHASE REQUEST
    // PENDING_APPROVAL -> APPROVED
    // ---------------------------------------------------------

    @Transactional
    public PurchaseRequest approvePurchaseRequest(UUID purchaseRequestId) {

        Employee approver = getAuthenticatedEmployee();

        PurchaseRequest purchaseRequest =
                getPurchaseRequestForUpdate(purchaseRequestId);

        validateSameCompany(purchaseRequest, approver);
        validateAssignedApprover(purchaseRequest, approver);

        if (purchaseRequest.getRequestStatus()
                != RequestStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                    "Only PENDING_APPROVAL requests can be approved");
        }

        purchaseRequest.setRequestStatus(RequestStatus.APPROVED);
        purchaseRequest.setRejectionReason(null);

        recordDecision(
                purchaseRequest,
                approver,
                PurchaseRequestDecisionType.APPROVED,
                RequestStatus.PENDING_APPROVAL,
                RequestStatus.APPROVED,
                null
        );

        return purchaseRequestRepo.save(purchaseRequest);
    }

    // ---------------------------------------------------------
    // REJECT PURCHASE REQUEST
    // PENDING_APPROVAL -> REJECTED
    // ---------------------------------------------------------

    @Transactional
    public PurchaseRequest rejectPurchaseRequest(
            UUID purchaseRequestId,
            String reason) {

        Employee approver = getAuthenticatedEmployee();

        PurchaseRequest purchaseRequest =
                getPurchaseRequestForUpdate(purchaseRequestId);

        validateSameCompany(purchaseRequest, approver);
        validateAssignedApprover(purchaseRequest, approver);

        if (purchaseRequest.getRequestStatus()
                != RequestStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                    "Only PENDING_APPROVAL requests can be rejected");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "A rejection reason is mandatory");
        }

        String rejectionReason = reason.trim();

        if (rejectionReason.length() > 1000) {
            throw new IllegalArgumentException(
                    "Rejection reason cannot exceed 1000 characters");
        }

        purchaseRequest.setRequestStatus(RequestStatus.REJECTED);
        purchaseRequest.setRejectionReason(rejectionReason);

        recordDecision(
                purchaseRequest,
                approver,
                PurchaseRequestDecisionType.REJECTED,
                RequestStatus.PENDING_APPROVAL,
                RequestStatus.REJECTED,
                rejectionReason
        );

        return purchaseRequestRepo.save(purchaseRequest);
    }

    // ---------------------------------------------------------
    // REVISE REJECTED REQUEST
    // REJECTED -> DRAFT
    // ---------------------------------------------------------

    @Transactional
    public PurchaseRequest revisePurchaseRequest(UUID purchaseRequestId) {

        Employee submitter = getAuthenticatedEmployee();

        PurchaseRequest purchaseRequest =
                getPurchaseRequestForUpdate(purchaseRequestId);

        validateSameCompany(purchaseRequest, submitter);
        validateSubmitter(purchaseRequest, submitter);

        if (purchaseRequest.getRequestStatus() != RequestStatus.REJECTED) {
            throw new IllegalStateException(
                    "Only REJECTED requests can be revised");
        }

        purchaseRequest.setRequestStatus(RequestStatus.DRAFT);
        purchaseRequest.setRejectionReason(null);
        purchaseRequest.setProcurementApprovalManager(null);
        purchaseRequest.setSubmissionDate(null);

        recordDecision(
                purchaseRequest,
                submitter,
                PurchaseRequestDecisionType.REVISED,
                RequestStatus.REJECTED,
                RequestStatus.DRAFT,
                null
        );

        return purchaseRequestRepo.save(purchaseRequest);
    }

    // ---------------------------------------------------------
    // CANCEL REQUEST
    // DRAFT/PENDING_APPROVAL -> CANCELLED
    // ---------------------------------------------------------

    @Transactional
    public PurchaseRequest cancelPurchaseRequest(UUID purchaseRequestId) {

        Employee submitter = getAuthenticatedEmployee();

        PurchaseRequest purchaseRequest =
                getPurchaseRequestForUpdate(purchaseRequestId);

        validateSameCompany(purchaseRequest, submitter);
        validateSubmitter(purchaseRequest, submitter);

        RequestStatus currentStatus =
                purchaseRequest.getRequestStatus();

        if (currentStatus != RequestStatus.DRAFT &&
                currentStatus != RequestStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                    "Only DRAFT or PENDING_APPROVAL requests can be cancelled");
        }

        purchaseRequest.setRequestStatus(RequestStatus.CANCELLED);

        recordDecision(
                purchaseRequest,
                submitter,
                PurchaseRequestDecisionType.CANCELLED,
                currentStatus,
                RequestStatus.CANCELLED,
                null
        );

        return purchaseRequestRepo.save(purchaseRequest);
    }

    // ---------------------------------------------------------
    // GET DECISION HISTORY
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PurchaseRequestDecision> getDecisionHistory(
            UUID purchaseRequestId) {

        Employee actor = getAuthenticatedEmployee();

        PurchaseRequest purchaseRequest =
                purchaseRequestRepo.findById(purchaseRequestId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Purchase request not found"));

        validateSameCompany(purchaseRequest, actor);

        return decisionRepo
                .findByPurchaseRequest_SysIdOrderByDecisionAtAsc(
                        purchaseRequestId);
    }

    // ---------------------------------------------------------
    // COMMON HELPERS
    // ---------------------------------------------------------

    private Employee getAuthenticatedEmployee() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof Claims claims)) {
            throw new AccessDeniedException(
                    "Authenticated employee information is unavailable");
        }

        String userId = claims.get("userId", String.class);
        String companyId = claims.get("companyId", String.class);

        if (userId == null || companyId == null) {
            throw new AccessDeniedException(
                    "Employee or company information is missing from token");
        }

        UUID employeeId;
        UUID tokenCompanyId;

        try {
            employeeId = UUID.fromString(userId);
            tokenCompanyId = UUID.fromString(companyId);
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException(
                    "Invalid authentication details");
        }

        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Authenticated employee does not exist"));

        if (employee.getCompany() == null ||
                !employee.getCompany().getSysId().equals(tokenCompanyId)) {
            throw new AccessDeniedException(
                    "Employee does not belong to the authenticated company");
        }

        if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE ||
                employee.getUserStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Employee account is not active");
        }

        return employee;
    }

    private PurchaseRequest getPurchaseRequestForUpdate(UUID id) {

        return purchaseRequestRepo.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Purchase request not found"));
    }

    private void validateSameCompany(
            PurchaseRequest purchaseRequest,
            Employee actor) {

        if (purchaseRequest.getCompany() == null ||
                actor.getCompany() == null ||
                !purchaseRequest.getCompany().getSysId()
                        .equals(actor.getCompany().getSysId())) {

            throw new AccessDeniedException(
                    "You cannot access another company's purchase request");
        }
    }

    private void validateSubmitter(
            PurchaseRequest purchaseRequest,
            Employee actor) {

        if (purchaseRequest.getSubmitterEmployee() == null ||
                !purchaseRequest.getSubmitterEmployee().getSysId()
                        .equals(actor.getSysId())) {

            throw new AccessDeniedException(
                    "Only the purchase request submitter can perform this action");
        }
    }

    private void validateApprovalManager(
            Employee manager,
            Employee submitter) {

        if (manager == null) {
            throw new IllegalStateException(
                    "The submitter does not have an approval manager assigned");
        }

        if (manager.getSysId().equals(submitter.getSysId())) {
            throw new AccessDeniedException(
                    "An employee cannot approve their own purchase request");
        }

        if (manager.getCompany() == null ||
                !manager.getCompany().getSysId()
                        .equals(submitter.getCompany().getSysId())) {
            throw new AccessDeniedException(
                    "Approval manager must belong to the same company");
        }

        if (manager.getEmploymentStatus() != EmploymentStatus.ACTIVE ||
                manager.getUserStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Approval manager is not active");
        }

        if (!hasOperation(manager, APPROVE_PURCHASE_REQUEST)) {
            throw new AccessDeniedException(
                    "Assigned manager does not have purchase request approval permission");
        }
    }

    private void validateAssignedApprover(
            PurchaseRequest purchaseRequest,
            Employee approver) {

        Employee assignedManager =
                purchaseRequest.getProcurementApprovalManager();

        if (assignedManager == null ||
                !assignedManager.getSysId().equals(approver.getSysId())) {
            throw new AccessDeniedException(
                    "Only the assigned approval manager can approve or reject this request");
        }

        if (!hasOperation(approver, APPROVE_PURCHASE_REQUEST)) {
            throw new AccessDeniedException(
                    "You do not have purchase request approval permission");
        }
    }

    private boolean hasOperation(Employee employee, String operationName) {

        if (employee.getRoles() == null) {
            return false;
        }

        return employee.getRoles().stream()
                .filter(role -> role.getOperations() != null)
                .flatMap(role -> role.getOperations().stream())
                .map(Operation::getOperationName)
                .anyMatch(operationName::equals);
    }

    private void recordDecision(
            PurchaseRequest purchaseRequest,
            Employee actor,
            PurchaseRequestDecisionType decisionType,
            RequestStatus fromStatus,
            RequestStatus toStatus,
            String reason) {

        PurchaseRequestDecision decision =
                new PurchaseRequestDecision();

        decision.setPurchaseRequest(purchaseRequest);
        decision.setActor(actor);
        decision.setDecisionType(decisionType);
        decision.setFromStatus(fromStatus);
        decision.setToStatus(toStatus);
        decision.setReason(reason);
        decision.setDecisionAt(LocalDateTime.now());

        decisionRepo.save(decision);
    }

    @Transactional
    public PurchaseRequest updateDraftPurchaseRequest(
            UUID purchaseRequestId,
            PurchaseRequestUpdateRequest request) {

        Employee actor = getAuthenticatedEmployee();

        PurchaseRequest purchaseRequest =
                getPurchaseRequestForUpdate(purchaseRequestId);

        validateSameCompany(purchaseRequest, actor);
        validateSubmitter(purchaseRequest, actor);

        if (purchaseRequest.getRequestStatus() != RequestStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT purchase requests can be edited");
        }

        purchaseRequest.setTitle(request.getTitle().trim());
        purchaseRequest.setDescription(request.getDescription());
        purchaseRequest.setPriority(request.getPriority());
        purchaseRequest.setRequiredDate(request.getRequiredDate());
        purchaseRequest.setDeliveryLocation(request.getDeliveryLocation());
        purchaseRequest.setCurrencyCode(request.getCurrencyCode());
        purchaseRequest.setBudgetCode(request.getBudgetCode());
        purchaseRequest.setPurchaseRequestType(
                request.getPurchaseRequestType());
        purchaseRequest.setProcurementType(request.getProcurementType());

        List<PurchaseRequestLine> existingLines =
                purchaseRequestLineRepo.findByPurchaseRequest_SysId(
                        purchaseRequestId);

        purchaseRequestLineRepo.deleteAll(existingLines);

        List<PurchaseRequestLine> newLines = new ArrayList<>();
        Set<UUID> addedProductIds = new HashSet<>();

        BigDecimal totalAmount = BigDecimal.ZERO;
        int lineNumber = 1;

        for (PurchaseRequestLineRequest lineRequest :
                request.getRequestLines()) {

            Product product = productRepo.findById(lineRequest.getProductId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Product does not exist: "
                                            + lineRequest.getProductId()));

            if (!addedProductIds.add(product.getSysId())) {
                throw new IllegalArgumentException(
                        "Duplicate product in purchase request");
            }

            if (product.getStatus() != ProductStatus.ACTIVE ||
                    product.getSupplierCompany() == null ||
                    product.getSupplierCompany().getStatus()
                            != CompanyStatus.ACTIVE) {
                throw new IllegalArgumentException(
                        "Only products from active suppliers can be requested");
            }

            if (product.getDescription() == null ||
                    product.getDescription().isBlank()) {
                throw new IllegalArgumentException(
                        "Product description is required");
            }

            if (lineRequest.getQuantity() == null ||
                    lineRequest.getQuantity().signum() <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be greater than zero");
            }

            if (lineRequest.getEstimatedUnitPrice() == null ||
                    lineRequest.getEstimatedUnitPrice().signum() < 0) {
                throw new IllegalArgumentException(
                        "Estimated unit price cannot be negative");
            }

            BigDecimal lineAmount =
                    lineRequest.getQuantity()
                            .multiply(lineRequest.getEstimatedUnitPrice())
                            .setScale(4, RoundingMode.HALF_UP);

            PurchaseRequestLine line = new PurchaseRequestLine();

            line.setPurchaseRequest(purchaseRequest);
            line.setLineNumber(lineNumber++);
            line.setProduct(product);
            line.setQuantity(lineRequest.getQuantity());
            line.setNotes(lineRequest.getNotes());
            line.setLineStatus(RequestLineStatus.OPEN);

            line.setItemDescription(product.getDescription());
            line.setUnitOfMeasure(product.getUnitOfMeasure());

            line.setEstimatedUnitPrice(lineRequest.getEstimatedUnitPrice());
            line.setEstimatedLineAmount(lineAmount);
            line.setCurrencyCode(request.getCurrencyCode());

            newLines.add(line);
            totalAmount = totalAmount.add(lineAmount);
        }

        purchaseRequestLineRepo.saveAll(newLines);

        purchaseRequest.setEstimatedTotalAmount(
                totalAmount.setScale(4, RoundingMode.HALF_UP));

        return purchaseRequestRepo.save(purchaseRequest);
    }
}