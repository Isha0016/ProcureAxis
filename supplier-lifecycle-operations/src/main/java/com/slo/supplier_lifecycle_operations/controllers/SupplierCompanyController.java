package com.slo.supplier_lifecycle_operations.controllers;

import com.fsc.supply_chain_common_operations.models.SupplierCompany;
import com.slo.supplier_lifecycle_operations.dtos.requests.SupplierCompanyRegistrationRequest;
import com.slo.supplier_lifecycle_operations.services.SupplierCompanyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import com.slo.supplier_lifecycle_operations.dtos.requests.SupplierCompanyUpdateRequest;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import com.slo.supplier_lifecycle_operations.dtos.requests.SupplierCompanyRejectionRequest;
import com.fsc.supply_chain_common_operations.services.AuthService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@Slf4j
@RestController
@RequestMapping("/api/v1/slo/company")
public class SupplierCompanyController {
    private AuthService authService;
    private SupplierCompanyService supplierCompanyService;

    @Autowired
    public SupplierCompanyController(SupplierCompanyService supplierCompanyService, AuthService authService){
        this.supplierCompanyService = supplierCompanyService;
        this.authService = authService;
    }

   @PostMapping("/register")
   public ResponseEntity registerSupplierCompany(
           @Valid @RequestBody SupplierCompanyRegistrationRequest supplierCompanyRegistrationRequest
           ){
       log.info("Received request to register supplier company: companyName={}",
               supplierCompanyRegistrationRequest.getCompanyName());
        SupplierCompany supplierCompany = supplierCompanyService.registerSupplierCompany(supplierCompanyRegistrationRequest);
       log.info("Supplier company registered successfully: companyName={}",
               supplierCompany.getCompanyName());
        return new ResponseEntity(supplierCompany, HttpStatus.CREATED);
   }
    @PostMapping("/{companyId}/reject")
    public ResponseEntity<SupplierCompany> rejectSupplierCompany(
            @PathVariable UUID companyId,@RequestHeader("token") String token,
            @Valid @RequestBody SupplierCompanyRejectionRequest request) {
        authService.isUserHavingAccess(token, "APPROVE_SUPPLIER");
        SupplierCompany rejectedCompany =
                supplierCompanyService.rejectSupplierCompany(
                        companyId,
                        request.getRejectionReason()
                );

        return ResponseEntity.ok(rejectedCompany);
    }
    @PostMapping("/{companyId}/review")
    public ResponseEntity<SupplierCompany> startSupplierCompanyReview(
            @PathVariable UUID companyId,@RequestHeader("token") String token) {
        authService.isUserHavingAccess(token, "APPROVE_SUPPLIER");
        SupplierCompany company =
                supplierCompanyService.startSupplierCompanyReview(companyId);

        return ResponseEntity.ok(company);
    }
    @PostMapping("/{companyId}/approve")
    public ResponseEntity<SupplierCompany> approveSupplierCompany(
            @PathVariable UUID companyId,@RequestHeader("token") String token) {
        authService.isUserHavingAccess(token, "APPROVE_SUPPLIER");
        SupplierCompany approvedCompany =
                supplierCompanyService.approveSupplierCompany(companyId);

        return ResponseEntity.ok(approvedCompany);
    }
    @PostMapping("/{companyId}/suspend")
    public ResponseEntity<SupplierCompany> suspendSupplierCompany(
            @PathVariable UUID companyId,@RequestHeader("token") String token) {
        authService.isUserHavingAccess(token, "APPROVE_SUPPLIER");
        SupplierCompany suspendedCompany =
                supplierCompanyService.suspendSupplierCompany(companyId);

        return ResponseEntity.ok(suspendedCompany);
    }
    @PostMapping("/{companyId}/deactivate")
    public ResponseEntity<SupplierCompany> deactivateSupplierCompany(
            @PathVariable UUID companyId,@RequestHeader("token") String token) {
        authService.isUserHavingAccess(token, "APPROVE_SUPPLIER");
        SupplierCompany deactivatedCompany =
                supplierCompanyService.deactivateSupplierCompany(companyId);

        return ResponseEntity.ok(deactivatedCompany);
    }
    @GetMapping("/{companyId}")
    public ResponseEntity<SupplierCompany> getSupplierCompanyById(
            @PathVariable UUID companyId) {

        SupplierCompany company =
                supplierCompanyService.getSupplierCompanyById(companyId);

        return ResponseEntity.ok(company);
    }
    @PatchMapping("/{companyId}")
    public ResponseEntity<SupplierCompany> updateSupplierCompany(
            @PathVariable UUID companyId,
            @Valid @RequestBody SupplierCompanyUpdateRequest request) {

        SupplierCompany updatedCompany =
                supplierCompanyService.updateSupplierCompany(
                        companyId, request);

        return ResponseEntity.ok(updatedCompany);
    }
}
