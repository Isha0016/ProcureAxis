package com.spo.sourcing_and_procurement_operations.services;

import com.fsc.supply_chain_common_operations.enums.CompanyStatus;
import com.fsc.supply_chain_common_operations.enums.CompanyType;
import com.fsc.supply_chain_common_operations.enums.ProductStatus;
import com.fsc.supply_chain_common_operations.enums.RequestLineStatus;
import com.fsc.supply_chain_common_operations.enums.RequestStatus;
import com.fsc.supply_chain_common_operations.models.Company;
import com.fsc.supply_chain_common_operations.models.Employee;
import com.fsc.supply_chain_common_operations.models.Product;
import com.fsc.supply_chain_common_operations.models.PurchaseRequest;
import com.fsc.supply_chain_common_operations.models.PurchaseRequestLine;
import com.fsc.supply_chain_common_operations.repositories.EmployeeRepo;
import com.fsc.supply_chain_common_operations.repositories.ProductRepo;
import com.spo.sourcing_and_procurement_operations.dtos.requests.PurchaseRequestCreateRequest;
import com.spo.sourcing_and_procurement_operations.dtos.requests.PurchaseRequestLineRequest;
import com.spo.sourcing_and_procurement_operations.repositories.PurchaseRequestLineRepo;
import com.spo.sourcing_and_procurement_operations.repositories.PurchaseRequestRepo;
import io.jsonwebtoken.Claims;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

@Service
public class PurchaseRequestService {

    private final EmployeeRepo employeeRepo;
    private final ProductRepo productRepo;
    private final PurchaseRequestRepo purchaseRequestRepo;
    private final PurchaseRequestLineRepo purchaseRequestLineRepo;

    public PurchaseRequestService(
            EmployeeRepo employeeRepo,
            ProductRepo productRepo,
            PurchaseRequestRepo purchaseRequestRepo,
            PurchaseRequestLineRepo purchaseRequestLineRepo) {

        this.employeeRepo = employeeRepo;
        this.productRepo = productRepo;
        this.purchaseRequestRepo = purchaseRequestRepo;
        this.purchaseRequestLineRepo = purchaseRequestLineRepo;
    }

    @Transactional
    public PurchaseRequest createPurchaseRequest(
            PurchaseRequestCreateRequest request) {

        // 1. Retrieve the authenticated employee
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
            throw new AccessDeniedException("Invalid authentication details");
        }

        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Authenticated employee does not exist"));

        // 2. Validate the employee's company
        Company company = employee.getCompany();

        if (company == null ||
                !company.getSysId().equals(tokenCompanyId)) {
            throw new AccessDeniedException(
                    "Employee does not belong to the authenticated company");
        }

        if (company.getCompanyType() != CompanyType.PROCUREMENT_COMPANY ||
                company.getStatus() != CompanyStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Only active procurement companies can create purchase requests");
        }

        // 3. Create the purchase request
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

        // 4. Create purchase request lines
        List<PurchaseRequestLine> requestLines = new ArrayList<>();
        Set<UUID> addedProductIds = new HashSet<>();
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

            // 5. Only active products from active suppliers are allowed
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


            // 6. Create the request line
            PurchaseRequestLine requestLine =
                    new PurchaseRequestLine();

            requestLine.setPurchaseRequest(savedRequest);
            requestLine.setLineNumber(lineNumber++);
            requestLine.setProduct(product);
            requestLine.setQuantity(lineRequest.getQuantity());
            requestLine.setNotes(lineRequest.getNotes());
            requestLine.setLineStatus(RequestLineStatus.OPEN);

            // 7. HISTORICAL PRODUCT SNAPSHOT
            // Copy values from the database, not from the client.
            requestLine.setItemDescription(product.getDescription());
            requestLine.setUnitOfMeasure(product.getUnitOfMeasure());

            requestLines.add(requestLine);
        }

        // 8. Save all request lines
        purchaseRequestLineRepo.saveAll(requestLines);

        return savedRequest;
    }
    @Transactional(readOnly = true)
    public PurchaseRequest getPurchaseRequest(UUID purchaseRequestId) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof Claims claims)) {
            throw new AccessDeniedException("Authentication required");
        }

        String companyId = claims.get("companyId", String.class);

        if (companyId == null || companyId.isBlank()) {
            throw new AccessDeniedException("Company ID missing from token");
        }

        UUID authenticatedCompanyId;

        try {
            authenticatedCompanyId = UUID.fromString(companyId);
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Invalid company ID");
        }

        PurchaseRequest purchaseRequest =
                purchaseRequestRepo.findById(purchaseRequestId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Purchase request not found"));

        if (purchaseRequest.getCompany() == null ||
                !purchaseRequest.getCompany().getSysId()
                        .equals(authenticatedCompanyId)) {
            throw new AccessDeniedException(
                    "You cannot access another company's purchase request");
        }

        return purchaseRequest;
    }
}