package com.spo.sourcing_and_procurement_operations.Controllers;

import com.fsc.supply_chain_common_operations.models.*;
import com.spo.sourcing_and_procurement_operations.dtos.requests.*;
import com.spo.sourcing_and_procurement_operations.dtos.requests.SourcingRequestCloseRequest;
import com.spo.sourcing_and_procurement_operations.dtos.responses.*;
import com.spo.sourcing_and_procurement_operations.repositories.*;
import com.spo.sourcing_and_procurement_operations.services.SourcingRequestService;
import com.spo.sourcing_and_procurement_operations.transformer.SourcingRequestTransformer;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/spo/sourcing-requests")
public class SourcingRequestController {

    private final SourcingRequestService service;
    private final SourcingRequestTransformer transformer;
    private final SourcingRequestLineRepo lineRepo;
    private final SourcingSupplierInvitationRepo invitationRepo;

    public SourcingRequestController(
            SourcingRequestService service,
            SourcingRequestTransformer transformer,
            SourcingRequestLineRepo lineRepo,
            SourcingSupplierInvitationRepo invitationRepo) {

        this.service = service;
        this.transformer = transformer;
        this.lineRepo = lineRepo;
        this.invitationRepo = invitationRepo;
    }

    // Create sourcing draft from an APPROVED purchase request.
    @PostMapping("/from-purchase-request/{purchaseRequestId}")
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<SourcingRequestResponse> createFromPurchaseRequest(
            @PathVariable UUID purchaseRequestId,
            @Valid @RequestBody SourcingRequestCreateRequest request) {

        SourcingRequest sr =
                service.createFromApprovedPurchaseRequest(
                        purchaseRequestId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(sr));
    }

    // Buyer: list all sourcing requests belonging to its company.
    @GetMapping
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<List<SourcingRequestResponse>> listBuyerRequests() {

        return ResponseEntity.ok(
                service.listForBuyer()
                        .stream()
                        .map(this::response)
                        .toList());
    }

    // Supplier: list only its own published invitations.
    @GetMapping("/invited")
    @PreAuthorize("@companyAccess.isSupplierCompany(authentication)")
    public ResponseEntity<List<SourcingRequestResponse>>
    listInvitedSupplierRequests() {

        return ResponseEntity.ok(
                service.listInvitedForSupplier()
                        .stream()
                        .map(sr -> response(sr, true))
                        .toList());
    }



    // Supplier: view a request only if its company was invited.
    @GetMapping("/supplier/{id}")
    @PreAuthorize("@companyAccess.isSupplierCompany(authentication)")
    public ResponseEntity<SourcingRequestResponse> getForSupplier(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                response(service.getForSupplier(id), true));
    }

    // Edit a DRAFT sourcing request.
    @PatchMapping("/{id}")
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<SourcingRequestResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody SourcingRequestUpdateRequest request) {

        return ResponseEntity.ok(
                response(service.updateDraft(id, request)));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<SourcingRequestResponse> submit(
            @PathVariable UUID id) {

        return ResponseEntity.ok(response(service.submit(id)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<SourcingRequestResponse> approve(
            @PathVariable UUID id) {

        return ResponseEntity.ok(response(service.approve(id)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<SourcingRequestResponse> reject(
            @PathVariable UUID id,
            @Valid @RequestBody PurchaseRequestDecisionRequest request) {

        return ResponseEntity.ok(
                response(service.reject(id, request.getReason())));
    }

    @PostMapping("/{id}/revise")
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<SourcingRequestResponse> revise(
            @PathVariable UUID id) {

        return ResponseEntity.ok(response(service.revise(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<SourcingRequestResponse> cancel(
            @PathVariable UUID id) {

        return ResponseEntity.ok(response(service.cancel(id)));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<SourcingRequestResponse> publish(
            @PathVariable UUID id) {

        return ResponseEntity.ok(response(service.publish(id)));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<SourcingRequestResponse> close(
            @PathVariable UUID id,
            @Valid @RequestBody SourcingRequestCloseRequest request) {

        return ResponseEntity.ok(
                response(service.close(id, request)));
    }

    // Buyer: view sourcing request decision history.
    @GetMapping("/{id}/history")
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<List<SourcingRequestDecisionResponse>> history(
            @PathVariable UUID id) {

        List<SourcingRequestDecisionResponse> result =
                service.history(id)
                        .stream()
                        .map(d -> SourcingRequestDecisionResponse.builder()
                                .sysId(d.getSysId())
                                .actorEmployeeSysId(d.getActor().getSysId())
                                .actorName(
                                        d.getActor().getFirstName()
                                                + " "
                                                + d.getActor().getLastName())
                                .decisionType(d.getDecisionType())
                                .fromStatus(d.getFromStatus())
                                .toStatus(d.getToStatus())
                                .reason(d.getReason())
                                .decisionAt(d.getDecisionAt())
                                .build())
                        .toList();

        return ResponseEntity.ok(result);
    }

    private SourcingRequestResponse response(SourcingRequest sr) {
        return response(sr, false);
    }

    private SourcingRequestResponse response(
            SourcingRequest sr, boolean supplierView) {

        List<SourcingRequestLine> lines =
                lineRepo.findBySourcingRequest_SysIdOrderByLineNumberAsc(
                        sr.getSysId());

        List<SourcingSupplierInvitation> invitations =
                supplierView
                        ? List.of()
                        : invitationRepo
                        .findBySourcingRequest_SysIdAndStatus(
                                sr.getSysId(),
                                com.fsc.supply_chain_common_operations.enums
                                .SourcingInvitationStatus.INVITED);

        SourcingRequestResponse result =
                transformer.toResponse(sr, lines, invitations);

        if (supplierView) {
            // Do not expose buyer-only details or competing invitees.
            result.setSourcePurchaseRequestSysId(null);
            result.setSourcePurchaseRequestNumber(null);
            result.setCompanySysId(null);
            result.setSubmitterEmployeeSysId(null);
            result.setProcurementApprovalManagerSysId(null);
            result.setTargetSupplierCount(null);
            result.setInvitedSuppliers(List.of());
        }

        return result;
    }
}