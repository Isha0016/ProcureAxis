package com.spo.sourcing_and_procurement_operations.services;

import com.fsc.supply_chain_common_operations.enums.*;
import com.fsc.supply_chain_common_operations.models.*;
import com.fsc.supply_chain_common_operations.repositories.EmployeeRepo;
import com.fsc.supply_chain_common_operations.repositories.CompanyRepo;
import com.fsc.supply_chain_common_operations.repositories.SourcingRequestDecisionRepo;
import com.spo.sourcing_and_procurement_operations.dtos.requests.SourcingRequestCloseRequest;
import com.spo.sourcing_and_procurement_operations.dtos.requests.SourcingRequestCreateRequest;
import com.spo.sourcing_and_procurement_operations.dtos.requests.SourcingRequestUpdateRequest;
import com.spo.sourcing_and_procurement_operations.repositories.*;

import io.jsonwebtoken.Claims;
import org.springframework.cglib.core.Local;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SourcingRequestService {

    private final EmployeeRepo employeeRepo;
    private final CompanyRepo companyRepo;
    private final PurchaseRequestRepo purchaseRequestRepo;
    private final PurchaseRequestLineRepo purchaseRequestLineRepo;
    private final SourcingRequestRepo sourcingRequestRepo;
    private final SourcingRequestLineRepo sourcingRequestLineRepo;
    private final SourcingSupplierInvitationRepo invitationRepo;
    private final SourcingRequestDecisionRepo decisionRepo;


    public SourcingRequestService(
            EmployeeRepo employeeRepo,
            CompanyRepo companyRepo,
            PurchaseRequestRepo purchaseRequestRepo,
            PurchaseRequestLineRepo purchaseRequestLineRepo,
            SourcingRequestRepo sourcingRequestRepo,
            SourcingRequestLineRepo sourcingRequestLineRepo,
            SourcingSupplierInvitationRepo invitationRepo,
            SourcingRequestDecisionRepo decisionRepo) {

        this.employeeRepo = employeeRepo;
        this.companyRepo = companyRepo;
        this.purchaseRequestRepo = purchaseRequestRepo;
        this.purchaseRequestLineRepo = purchaseRequestLineRepo;
        this.sourcingRequestRepo = sourcingRequestRepo;
        this.sourcingRequestLineRepo = sourcingRequestLineRepo;
        this.invitationRepo = invitationRepo;
        this.decisionRepo = decisionRepo;
    }

    // F07 -> F08: create sourcing request from an APPROVED purchase request.
    @Transactional
    public SourcingRequest createFromApprovedPurchaseRequest(
            UUID purchaseRequestId,
            SourcingRequestCreateRequest request) {

        Employee actor = authenticatedEmployee();

        requireActiveProcurementCompany(actor);
        requireOperation(actor, "CREATE_SOURCING_REQUEST");

        PurchaseRequest pr = purchaseRequestRepo.findById(purchaseRequestId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Purchase request not found"));

        requireSameCompany(pr.getCompany(), actor.getCompany());

        if (pr.getRequestStatus() != RequestStatus.APPROVED) {
            throw new IllegalStateException(
                    "Sourcing can only be created from an APPROVED purchase request");
        }

        if (sourcingRequestRepo.existsBySourcePurchaseRequest_SysId(purchaseRequestId)) {
            throw new IllegalStateException(
                    "A sourcing request already exists for this purchase request");
        }

        List<PurchaseRequestLine> sourceLines =
                purchaseRequestLineRepo.findByPurchaseRequest_SysId(purchaseRequestId);

        if (sourceLines == null || sourceLines.isEmpty()) {
            throw new IllegalArgumentException(
                    "Approved purchase request has no lines");
        }

        validateSupplierIds(request.getSupplierCompanyIds());

        SourcingRequest sr = new SourcingRequest();

        sr.setRequestNumber(
                "SR-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT));
        sr.setTitle(request.getTitle().trim());
        sr.setDescription(request.getDescription());
        sr.setCompany(actor.getCompany());
        sr.setSourcePurchaseRequest(pr);
        sr.setSubmitterEmployee(actor);
        sr.setProcurementApprovalManager(pr.getProcurementApprovalManager());
        sr.setRequestStatus(RequestStatus.DRAFT);
        sr.setPriority(pr.getPriority());
        sr.setRequiredDate(pr.getRequiredDate());
        sr.setSourcingMethod(request.getSourcingMethod());
        sr.setCurrencyCode(pr.getCurrencyCode());
        sr.setEstimatedTotalAmount(pr.getEstimatedTotalAmount());
        sr.setDeliveryLocation(pr.getDeliveryLocation());
        sr.setBudgetCode(pr.getBudgetCode());
        sr.setProcurementType(pr.getProcurementType());
        sr.setEvaluationCriteria(request.getEvaluationCriteria());
        sr.setCommercialTerms(request.getCommercialTerms());
        sr.setResponseDeadline(request.getResponseDeadline());
        sr.setEvaluationDeadline(request.getEvaluationDeadline());
        sr.setTargetSupplierCount(request.getSupplierCompanyIds().size());

        sr = sourcingRequestRepo.save(sr);

        // Copy PR line information into sourcing-line snapshots.
        List<SourcingRequestLine> copiedLines = new ArrayList<>();
        int lineNo = 1;

        for (PurchaseRequestLine source : sourceLines) {

            SourcingRequestLine line = new SourcingRequestLine();

            line.setSourcingRequest(sr);
            line.setSourcePurchaseRequestLine(source);

            line.setSourceProductSysId(
                    source.getProduct() == null
                            ? null
                            : source.getProduct().getSysId());

            line.setLineNumber(lineNo++);

            // Product snapshots
            line.setItemDescription(source.getItemDescription());

            line.setProductCodeSnapshot(
                    source.getProduct() == null
                            ? null
                            : source.getProduct().getProductCode());

            // Quantity and unit
            line.setQuantity(source.getQuantity());
            line.setUnitOfMeasure(source.getUnitOfMeasure());
            line.setNotes(source.getNotes());

            // Commercial snapshots
            line.setEstimatedUnitPrice(source.getEstimatedUnitPrice());
            line.setEstimatedLineAmount(source.getEstimatedLineAmount());
            line.setCurrencyCode(source.getCurrencyCode());
            line.setRequiredDate(source.getRequiredDate());
            line.setDeliveryLocation(source.getDeliveryLocation());
            line.setTaxRate(source.getTaxRate());

            copiedLines.add(line);
        }

        sourcingRequestLineRepo.saveAll(copiedLines);

        saveInvitations(sr, request.getSupplierCompanyIds());

        return sr;
    }

    @Transactional
    public SourcingRequest updateDraft(
            UUID id,
            SourcingRequestUpdateRequest request) {

        Employee actor = authenticatedEmployee();
        SourcingRequest sr = getForUpdate(id);

        requireBuyerOwner(sr, actor);
        requireSubmitter(sr, actor);
        requireOperation(actor, "UPDATE_SOURCING_REQUEST");

        if (sr.getRequestStatus() != RequestStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT sourcing requests can be updated");
        }

        if (request.getTitle() != null) {
            if (request.getTitle().isBlank()) {
                throw new IllegalArgumentException("Title cannot be blank");
            }
            sr.setTitle(request.getTitle().trim());
        }

        if (request.getDescription() != null) {
            sr.setDescription(request.getDescription());
        }

        if (request.getSourcingMethod() != null) {
            sr.setSourcingMethod(request.getSourcingMethod());
        }

        if (request.getEvaluationCriteria() != null) {
            sr.setEvaluationCriteria(request.getEvaluationCriteria());
        }

        if (request.getCommercialTerms() != null) {
            sr.setCommercialTerms(request.getCommercialTerms());
        }

        if (request.getResponseDeadline() != null) {
            sr.setResponseDeadline(request.getResponseDeadline());
        }

        if (request.getEvaluationDeadline() != null) {
            sr.setEvaluationDeadline(request.getEvaluationDeadline());
        }

        if (request.getSupplierCompanyIds() != null) {
            validateSupplierIds(request.getSupplierCompanyIds());

            invitationRepo.deleteBySourcingRequest_SysId(id);

            saveInvitations(sr, request.getSupplierCompanyIds());

            sr.setTargetSupplierCount(
                    request.getSupplierCompanyIds().size());
        }

        return sourcingRequestRepo.save(sr);
    }

    @Transactional
    public SourcingRequest submit(UUID id) {

        Employee actor = authenticatedEmployee();
        SourcingRequest sr = getForUpdate(id);

        requireBuyerOwner(sr, actor);
        requireSubmitter(sr, actor);
        requireOperation(actor, "UPDATE_SOURCING_REQUEST");
        requireStatus(sr, RequestStatus.DRAFT);

        if (sr.getProcurementApprovalManager() == null) {
            throw new IllegalStateException(
                    "Purchase request has no assigned approval manager");
        }

        if (sr.getProcurementApprovalManager().getSysId()
                .equals(actor.getSysId())) {
            throw new IllegalStateException(
                    "Submitter cannot approve their own sourcing request");
        }

        validateSourcingApprovalManager(
                sr.getProcurementApprovalManager(), actor);

        if (sourcingRequestLineRepo
                .findBySourcingRequest_SysIdOrderByLineNumberAsc(id)
                .isEmpty()) {
            throw new IllegalStateException(
                    "Sourcing request must contain at least one line");
        }

        if (invitationRepo
                .findBySourcingRequest_SysIdAndStatus(
                        id, SourcingInvitationStatus.INVITED)
                .size() < 2) {
            throw new IllegalStateException(
                    "At least two active suppliers must be invited");
        }

        record(sr, actor, SourcingRequestDecisionType.SUBMITTED,
                RequestStatus.DRAFT, RequestStatus.SUBMITTED, null);

        sr.setRequestStatus(RequestStatus.SUBMITTED);

        record(sr, actor, SourcingRequestDecisionType.PENDING_APPROVAL_ASSIGNED,
                RequestStatus.SUBMITTED,
                RequestStatus.PENDING_APPROVAL, null);

        sr.setRequestStatus(RequestStatus.PENDING_APPROVAL);
        sr.setSubmissionDate(Instant.now());

        return sourcingRequestRepo.save(sr);
    }

    @Transactional
    public SourcingRequest approve(UUID id) {

        Employee actor = authenticatedEmployee();
        SourcingRequest sr = getForUpdate(id);

        requireBuyerOwner(sr, actor);
        requireOperation(actor, "APPROVE_PURCHASE_REQUEST");

        if (sr.getProcurementApprovalManager() == null
                || !sr.getProcurementApprovalManager().getSysId()
                .equals(actor.getSysId())) {
            throw new AccessDeniedException(
                    "Only the assigned sourcing approval manager can approve this request");
        }

        requireStatus(sr, RequestStatus.PENDING_APPROVAL);

        sr.setRequestStatus(RequestStatus.APPROVED);

        record(sr, actor, SourcingRequestDecisionType.APPROVED,
                RequestStatus.PENDING_APPROVAL,
                RequestStatus.APPROVED, null);

        return sourcingRequestRepo.save(sr);
    }

    @Transactional
    public SourcingRequest reject(UUID id, String reason) {

        Employee actor = authenticatedEmployee();
        SourcingRequest sr = getForUpdate(id);

        requireBuyerOwner(sr, actor);
        requireOperation(actor, "REJECT_PURCHASE_REQUEST");

        if (sr.getProcurementApprovalManager() == null
                || !sr.getProcurementApprovalManager().getSysId()
                .equals(actor.getSysId())) {
            throw new AccessDeniedException(
                    "Only the assigned sourcing approval manager can reject this request");
        }

        requireStatus(sr, RequestStatus.PENDING_APPROVAL);
        requireReason(reason);

        sr.setRequestStatus(RequestStatus.REJECTED);
        sr.setRejectionReason(reason.trim());

        record(sr, actor, SourcingRequestDecisionType.REJECTED,
                RequestStatus.PENDING_APPROVAL,
                RequestStatus.REJECTED, reason.trim());

        return sourcingRequestRepo.save(sr);
    }

    @Transactional
    public SourcingRequest revise(UUID id) {

        Employee actor = authenticatedEmployee();
        SourcingRequest sr = getForUpdate(id);

        requireBuyerOwner(sr, actor);
        requireSubmitter(sr, actor);
        requireOperation(actor, "UPDATE_SOURCING_REQUEST");
        requireStatus(sr, RequestStatus.REJECTED);

        sr.setRequestStatus(RequestStatus.DRAFT);
        sr.setRejectionReason(null);

        record(sr, actor, SourcingRequestDecisionType.REVISED,
                RequestStatus.REJECTED, RequestStatus.DRAFT, null);

        return sourcingRequestRepo.save(sr);
    }

    @Transactional
    public SourcingRequest cancel(UUID id) {

        Employee actor = authenticatedEmployee();
        SourcingRequest sr = getForUpdate(id);

        requireBuyerOwner(sr, actor);
        requireSubmitter(sr, actor);
        requireOperation(actor, "UPDATE_SOURCING_REQUEST");

        if (sr.getRequestStatus() != RequestStatus.DRAFT
                && sr.getRequestStatus() != RequestStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                    "Only DRAFT or PENDING_APPROVAL sourcing requests can be cancelled");
        }

        RequestStatus oldStatus = sr.getRequestStatus();

        sr.setRequestStatus(RequestStatus.CANCELLED);

        record(sr, actor, SourcingRequestDecisionType.CANCELLED,
                oldStatus, RequestStatus.CANCELLED, null);

        return sourcingRequestRepo.save(sr);
    }

    @Transactional
    public SourcingRequest publish(UUID id) {

        Employee actor = authenticatedEmployee();
        SourcingRequest sr = getForUpdate(id);

        // Only the owning procurement company can publish
        requireBuyerOwner(sr, actor);

        // Check permission
        requireOperation(actor, "PUBLISH_SOURCING_REQUEST");

        // Only APPROVED requests can be published
        requireStatus(sr, RequestStatus.APPROVED);

        Instant now = Instant.now();

        // 1. Validate response deadline
        if (sr.getResponseDeadline() == null ||
                !sr.getResponseDeadline().isAfter(now)) {
            throw new IllegalArgumentException(
                    "Response deadline must be in the future");
        }

        // 2. Validate evaluation deadline
        if (sr.getEvaluationDeadline() == null ||
                sr.getEvaluationDeadline().isBefore(sr.getResponseDeadline())) {
            throw new IllegalArgumentException(
                    "Evaluation deadline must be on or after the response deadline");
        }

        // 3. Validate currency
        if (sr.getCurrencyCode() == null ||
                !sr.getCurrencyCode().matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Sourcing request must have a valid currency code");
        }

        // 4. Validate sourcing request lines
        validateSourcingLines(id);

        // 5. Validate invited suppliers
        validateInvitedSuppliers(id);

        // 6. Publish the sourcing request
        sr.setRequestStatus(RequestStatus.IN_PROGRESS);
        sr.setPublishedAt(now);

        // 7. Record publication history
        record(
                sr,
                actor,
                SourcingRequestDecisionType.PUBLISHED,
                RequestStatus.APPROVED,
                RequestStatus.IN_PROGRESS,
                null
        );

        return sourcingRequestRepo.save(sr);
    }


    // Validate sourcing request lines
    private void validateSourcingLines(UUID sourcingRequestId) {

        List<SourcingRequestLine> lines =
                sourcingRequestLineRepo
                        .findBySourcingRequest_SysIdOrderByLineNumberAsc(
                                sourcingRequestId
                        );

        if (lines == null || lines.isEmpty()) {
            throw new IllegalStateException(
                    "Sourcing request must contain at least one line");
        }

        for (SourcingRequestLine line : lines) {

            if (line.getItemDescription() == null ||
                    line.getItemDescription().isBlank()) {
                throw new IllegalArgumentException(
                        "Every sourcing line must have an item description");
            }

            if (line.getQuantity() == null ||
                    line.getQuantity().signum() <= 0) {
                throw new IllegalArgumentException(
                        "Sourcing line quantity must be greater than zero");
            }

            if (line.getUnitOfMeasure() == null) {
                throw new IllegalArgumentException(
                        "Every sourcing line must have a unit of measure");
            }
        }
    }


    // Validate invited suppliers
    private void validateInvitedSuppliers(UUID sourcingRequestId) {

        List<SourcingSupplierInvitation> invitations =
                invitationRepo.findBySourcingRequest_SysIdAndStatus(
                        sourcingRequestId,
                        SourcingInvitationStatus.INVITED
                );

        Set<UUID> activeSupplierIds = new HashSet<>();

        for (SourcingSupplierInvitation invitation : invitations) {

            SupplierCompany supplier = invitation.getSupplierCompany();

            if (supplier != null &&
                    supplier.getStatus() == CompanyStatus.ACTIVE) {

                activeSupplierIds.add(supplier.getSysId());
            }
        }

        if (activeSupplierIds.size() < 2) {
            throw new IllegalStateException(
                    "At least two distinct ACTIVE suppliers must be invited");
        }
    }
    @Transactional
    public SourcingRequest close(
            UUID id,
            SourcingRequestCloseRequest request
    ) {
        Employee actor = authenticatedEmployee();

        SourcingRequest sr = getForUpdate(id);

        requireBuyerOwner(sr, actor);
        requireOperation(actor, "CLOSE_SOURCING_REQUEST");
        requireStatus(sr, RequestStatus.IN_PROGRESS);

        if (request.getClosureOutcome() == null) {
            throw new IllegalArgumentException(
                    "Closure outcome is required"
            );
        }

        // AWARDED must be recorded through the actual award workflow.
        // That workflow will be implemented in F09.
        if (request.getClosureOutcome() == SourcingClosureOutcome.AWARDED) {
            throw new IllegalStateException(
                    "An award must be recorded through the award workflow"
            );
        }

        if (request.getReason() == null ||
                request.getReason().isBlank()) {
            throw new IllegalArgumentException(
                    "A reason is required when closing without an award"
            );
        }

        sr.setClosureOutcome(SourcingClosureOutcome.NO_AWARD);
        sr.setClosureReason(request.getReason().trim());
        sr.setClosedAt(Instant.now());
        sr.setRequestStatus(RequestStatus.COMPLETED);

        record(
                sr,
                actor,
                SourcingRequestDecisionType.CLOSED,
                RequestStatus.IN_PROGRESS,
                RequestStatus.COMPLETED,
                request.getReason().trim()
        );

        return sourcingRequestRepo.save(sr);
    }

    @Transactional(readOnly = true)
    public SourcingRequest getForBuyer(UUID id) {

        Employee actor = authenticatedEmployee();

        SourcingRequest sr = sourcingRequestRepo.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Sourcing request not found"));

        requireBuyerOwner(sr, actor);
        requireOperation(actor, "VIEW_SOURCING_REQUEST");

        return sr;
    }

    @Transactional(readOnly = true)
    public SourcingRequest getForSupplier(UUID id) {

        Employee actor = authenticatedEmployee();

        requireActiveSupplierCompany(actor);
        requireOperation(actor, "VIEW_SOURCING_REQUEST");

        SourcingRequest sr = sourcingRequestRepo.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Sourcing request not found"));

        if (sr.getRequestStatus() != RequestStatus.IN_PROGRESS
                || sr.getPublishedAt() == null) {
            throw new AccessDeniedException(
                    "Sourcing request is not published");
        }

        invitationRepo
                .findBySourcingRequest_SysIdAndSupplierCompany_SysIdAndStatus(
                        id,
                        actor.getCompany().getSysId(),
                        SourcingInvitationStatus.INVITED)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Your company is not invited to this sourcing request"));

        return sr;
    }

    @Transactional(readOnly = true)
    public List<SourcingRequest> listForBuyer() {

        Employee actor = authenticatedEmployee();

        requireActiveProcurementCompany(actor);
        requireOperation(actor, "VIEW_SOURCING_REQUEST");

        return sourcingRequestRepo
                .findByCompany_SysIdOrderByCreatedAtDesc(
                        actor.getCompany().getSysId());
    }

    @Transactional(readOnly = true)
    public List<SourcingRequest> listInvitedForSupplier() {

        Employee actor = authenticatedEmployee();

        requireActiveSupplierCompany(actor);
        requireOperation(actor, "VIEW_SOURCING_REQUEST");

        return invitationRepo
                .findBySupplierCompany_SysIdAndStatus(
                        actor.getCompany().getSysId(),
                        SourcingInvitationStatus.INVITED)
                .stream()
                .map(SourcingSupplierInvitation::getSourcingRequest)
                .filter(sr ->
                        sr.getRequestStatus() == RequestStatus.IN_PROGRESS
                                && sr.getPublishedAt() != null)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SourcingRequestDecision> history(UUID id) {
        getForBuyer(id);
        return decisionRepo
                .findBySourcingRequest_SysIdOrderByDecisionAtAsc(id);
    }

    private void validateSupplierIds(List<UUID> ids) {

        if (ids == null || ids.size() < 2) {
            throw new IllegalArgumentException(
                    "At least two supplier companies must be invited");
        }

        if (new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException(
                    "Duplicate supplier company IDs are not allowed");
        }

        for (UUID supplierId : ids) {

            Company company = companyRepo.findById(supplierId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Supplier company not found: " + supplierId));

            if (company.getCompanyType() != CompanyType.SUPPLIER_COMPANY
                    || company.getStatus() != CompanyStatus.ACTIVE) {
                throw new IllegalArgumentException(
                        "Only ACTIVE supplier companies can be invited: " + supplierId);
            }
        }
    }

    private void saveInvitations(SourcingRequest sr, List<UUID> ids) {

        for (UUID supplierId : ids) {

            SupplierCompany supplier = (SupplierCompany) companyRepo
                    .findById(supplierId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Supplier company not found: " + supplierId));

            SourcingSupplierInvitation invitation =
                    new SourcingSupplierInvitation();

            invitation.setSourcingRequest(sr);
            invitation.setSupplierCompany(supplier);
            invitation.setStatus(SourcingInvitationStatus.INVITED);
            invitation.setInvitedAt(Instant.now());

            invitationRepo.save(invitation);
        }
    }

    private Employee authenticatedEmployee() {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null
                || !(auth.getPrincipal() instanceof Claims claims)) {
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
        } catch (IllegalArgumentException ex) {
            throw new AccessDeniedException(
                    "Invalid authentication details");
        }

        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Authenticated employee does not exist"));

        if (employee.getCompany() == null
                || !employee.getCompany().getSysId().equals(tokenCompanyId)) {
            throw new AccessDeniedException(
                    "Employee does not belong to authenticated company");
        }

        if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE
                || employee.getUserStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Employee account is not active");
        }

        return employee;
    }

    private void requireActiveProcurementCompany(Employee employee) {

        if (employee.getCompany().getCompanyType()
                != CompanyType.PROCUREMENT_COMPANY
                || employee.getCompany().getStatus() != CompanyStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Only employees of an ACTIVE procurement company can perform this action");
        }
    }

    private void requireActiveSupplierCompany(Employee employee) {

        if (employee.getCompany().getCompanyType()
                != CompanyType.SUPPLIER_COMPANY
                || employee.getCompany().getStatus() != CompanyStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Only employees of an ACTIVE supplier company can perform this action");
        }
    }

    private void requireBuyerOwner(SourcingRequest sr, Employee actor) {
        requireActiveProcurementCompany(actor);
        requireSameCompany(sr.getCompany(), actor.getCompany());
    }

    private void requireSameCompany(Company first, Company second) {

        if (first == null || second == null
                || !first.getSysId().equals(second.getSysId())) {
            throw new AccessDeniedException(
                    "You cannot access another company's record");
        }
    }

    private void requireSubmitter(SourcingRequest sr, Employee actor) {

        if (sr.getSubmitterEmployee() == null
                || !sr.getSubmitterEmployee().getSysId()
                .equals(actor.getSysId())) {
            throw new AccessDeniedException(
                    "Only the sourcing request submitter can perform this action");
        }
    }

    private SourcingRequest getForUpdate(UUID id) {
        return sourcingRequestRepo.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Sourcing request not found"));
    }

    private void validateSourcingApprovalManager(
            Employee manager, Employee submitter) {

        if (manager.getCompany() == null
                || !manager.getCompany().getSysId()
                .equals(submitter.getCompany().getSysId())) {
            throw new AccessDeniedException(
                    "Sourcing approval manager must belong to the same procurement company");
        }

        if (manager.getEmploymentStatus() != EmploymentStatus.ACTIVE
                || manager.getUserStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Sourcing approval manager is not active");
        }

        requireOperation(manager, "APPROVE_PURCHASE_REQUEST");
    }

    private void requireOperation(Employee employee, String operationName) {

        if (employee.getRoles() == null
                || employee.getRoles().stream()
                .filter(role -> role.getOperations() != null)
                .flatMap(role -> role.getOperations().stream())
                .map(Operation::getOperationName)
                .noneMatch(operationName::equals)) {

            throw new AccessDeniedException(
                    "You do not have permission: " + operationName);
        }
    }

    private void requireStatus(
            SourcingRequest sr, RequestStatus expectedStatus) {

        if (sr.getRequestStatus() != expectedStatus) {
            throw new IllegalStateException(
                    "Sourcing request must be in " + expectedStatus + " state");
        }
    }

    private void requireReason(String reason) {

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required");
        }

        if (reason.trim().length() > 1000) {
            throw new IllegalArgumentException(
                    "Reason cannot exceed 1000 characters");
        }
    }

    private void record(
            SourcingRequest sr,
            Employee actor,
            SourcingRequestDecisionType type,
            RequestStatus from,
            RequestStatus to,
            String reason) {

        SourcingRequestDecision decision =
                new SourcingRequestDecision();

        decision.setSourcingRequest(sr);
        decision.setActor(actor);
        decision.setDecisionType(type);
        decision.setFromStatus(from);
        decision.setToStatus(to);
        decision.setReason(reason);
        decision.setDecisionAt(LocalDateTime.now());

        decisionRepo.save(decision);
    }
}