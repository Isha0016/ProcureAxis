package com.spo.sourcing_and_procurement_operations.Controllers;

import com.fsc.supply_chain_common_operations.models.ProcurementCompany;
import com.fsc.supply_chain_common_operations.services.AuthService;
import com.spo.sourcing_and_procurement_operations.dtos.requests.ProcurementCompanyRegistrationRequest;
import com.spo.sourcing_and_procurement_operations.services.ProcurementCompanyService;
import com.spo.sourcing_and_procurement_operations.dtos.requests.ProcurementCompanyUpdateRequest;

import com.spo.sourcing_and_procurement_operations.dtos.requests.ProcurementCompanyReviewRequest;

import com.spo.sourcing_and_procurement_operations.dtos.responses.ProcurementCompanyResponse;
import com.spo.sourcing_and_procurement_operations.transformer.ProcurementCompanyTransformer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableDefault;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/procurement-companies")
public class ProcurementCompanyController {

    private  final ProcurementCompanyService procurementCompanyService;
    private final AuthService authService;

    public ProcurementCompanyController(
            ProcurementCompanyService procurementCompanyService,
            AuthService authService) {

        this.procurementCompanyService = procurementCompanyService;
        this.authService = authService;
    }


    @PostMapping("/register")
    public ResponseEntity<String> registerProcurementCompany(
            @Valid @RequestBody ProcurementCompanyRegistrationRequest request) {

        procurementCompanyService.registerProcurementCompany(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Procurement company registered successfully");
    }
    // GET ALL PROCUREMENT COMPANIES

    @GetMapping
    public ResponseEntity<Page<ProcurementCompanyResponse>> getAllCompanies(
            @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(
                procurementCompanyService.getAllCompanies(pageable)
        );
    }


// GET PROCUREMENT COMPANY BY ID

    @GetMapping("/{id}")
    public ResponseEntity<ProcurementCompanyResponse> getCompanyById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                procurementCompanyService.getCompanyById(id)
        );
    }


// UPDATE PROCUREMENT COMPANY PROFILE

    @PatchMapping("/{id}")
    public ResponseEntity<ProcurementCompanyResponse> updateCompany(
            @PathVariable UUID id,
            @RequestBody ProcurementCompanyUpdateRequest request) {

        return ResponseEntity.ok(
                procurementCompanyService.updateCompany(id, request)
        );
    }


// REVIEW PROCUREMENT COMPANY

    @PostMapping("/{id}/review")
    public ResponseEntity<ProcurementCompanyResponse> reviewCompany(
            @PathVariable UUID id, @RequestHeader("token") String token,
            @RequestBody ProcurementCompanyReviewRequest request) {
        authService.isUserHavingAccess(token, "APPROVE_PROCUREMENT");
        return ResponseEntity.ok(
                procurementCompanyService.reviewCompany(id, request)
        );
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<ProcurementCompanyResponse> suspendCompany(
            @PathVariable UUID id,
            @RequestHeader("token") String token) {

        authService.isUserHavingAccess(token, "APPROVE_PROCUREMENT");

        ProcurementCompany company =
                procurementCompanyService.suspendCompany(id);

        return ResponseEntity.ok(
                ProcurementCompanyTransformer.mapToResponse(company)
        );
    }
    @PostMapping("/{id}/reinstate")
    public ResponseEntity<ProcurementCompanyResponse> reinstateCompany(
            @PathVariable UUID id,
            @RequestHeader("token") String token) {

        authService.isUserHavingAccess(token, "APPROVE_PROCUREMENT");

        ProcurementCompany company =
                procurementCompanyService.reinstateCompany(id);

        return ResponseEntity.ok(
                ProcurementCompanyTransformer.mapToResponse(company)
        );
    }
}