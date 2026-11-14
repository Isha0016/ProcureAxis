package com.spo.sourcing_and_procurement_operations.Controllers;

import com.fsc.supply_chain_common_operations.models.PurchaseRequest;
import com.fsc.supply_chain_common_operations.models.PurchaseRequestDecision;
import com.fsc.supply_chain_common_operations.models.PurchaseRequestLine;
import com.spo.sourcing_and_procurement_operations.dtos.requests.PurchaseRequestCreateRequest;
import com.spo.sourcing_and_procurement_operations.dtos.requests.PurchaseRequestDecisionRequest;
import com.spo.sourcing_and_procurement_operations.dtos.requests.PurchaseRequestUpdateRequest;
import com.spo.sourcing_and_procurement_operations.dtos.responses.PurchaseRequestDecisionResponse;
import com.spo.sourcing_and_procurement_operations.dtos.responses.PurchaseRequestResponse;
import com.spo.sourcing_and_procurement_operations.repositories.PurchaseRequestLineRepo;
import com.spo.sourcing_and_procurement_operations.services.PurchaseRequestService;
import com.spo.sourcing_and_procurement_operations.transformer.PurchaseRequestTransformer;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/spo/purchase-requests")
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;
    private final PurchaseRequestTransformer purchaseRequestTransformer;
    private final PurchaseRequestLineRepo purchaseRequestLineRepo;

    public PurchaseRequestController(
            PurchaseRequestService purchaseRequestService,
            PurchaseRequestTransformer purchaseRequestTransformer,
            PurchaseRequestLineRepo purchaseRequestLineRepo) {

        this.purchaseRequestService = purchaseRequestService;
        this.purchaseRequestTransformer = purchaseRequestTransformer;
        this.purchaseRequestLineRepo = purchaseRequestLineRepo;
    }

    // CREATE DRAFT
    @PostMapping
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<PurchaseRequestResponse> createPurchaseRequest(
            @Valid @RequestBody PurchaseRequestCreateRequest request) {

        PurchaseRequest purchaseRequest =
                purchaseRequestService.createPurchaseRequest(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(purchaseRequest));
    }

    // GET PURCHASE REQUEST
    @GetMapping("/{purchaseRequestId}")
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<PurchaseRequestResponse> getPurchaseRequest(
            @PathVariable UUID purchaseRequestId) {

        PurchaseRequest purchaseRequest =
                purchaseRequestService.getPurchaseRequest(purchaseRequestId);

        return ResponseEntity.ok(toResponse(purchaseRequest));
    }

    // SUBMIT
    @PostMapping("/{purchaseRequestId}/submit")
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<PurchaseRequestResponse> submitPurchaseRequest(
            @PathVariable UUID purchaseRequestId) {

        PurchaseRequest purchaseRequest =
                purchaseRequestService.submitPurchaseRequest(purchaseRequestId);

        return ResponseEntity.ok(toResponse(purchaseRequest));
    }

    // APPROVE
    @PostMapping("/{purchaseRequestId}/approve")
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<PurchaseRequestResponse> approvePurchaseRequest(
            @PathVariable UUID purchaseRequestId) {

        PurchaseRequest purchaseRequest =
                purchaseRequestService.approvePurchaseRequest(
                        purchaseRequestId);

        return ResponseEntity.ok(toResponse(purchaseRequest));
    }

    // REJECT
    @PostMapping("/{purchaseRequestId}/reject")
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<PurchaseRequestResponse> rejectPurchaseRequest(
            @PathVariable UUID purchaseRequestId,
            @Valid @RequestBody PurchaseRequestDecisionRequest request) {

        PurchaseRequest purchaseRequest =
                purchaseRequestService.rejectPurchaseRequest(
                        purchaseRequestId,
                        request.getReason());

        return ResponseEntity.ok(toResponse(purchaseRequest));
    }

    // REVISE
    @PostMapping("/{purchaseRequestId}/revise")
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<PurchaseRequestResponse> revisePurchaseRequest(
            @PathVariable UUID purchaseRequestId) {

        PurchaseRequest purchaseRequest =
                purchaseRequestService.revisePurchaseRequest(
                        purchaseRequestId);

        return ResponseEntity.ok(toResponse(purchaseRequest));
    }

    // CANCEL
    @PostMapping("/{purchaseRequestId}/cancel")
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<PurchaseRequestResponse> cancelPurchaseRequest(
            @PathVariable UUID purchaseRequestId) {

        PurchaseRequest purchaseRequest =
                purchaseRequestService.cancelPurchaseRequest(
                        purchaseRequestId);

        return ResponseEntity.ok(toResponse(purchaseRequest));
    }

    // DECISION HISTORY
    @GetMapping("/{purchaseRequestId}/history")
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<List<PurchaseRequestDecisionResponse>>
    getDecisionHistory(@PathVariable UUID purchaseRequestId) {

        List<PurchaseRequestDecision> decisions =
                purchaseRequestService.getDecisionHistory(
                        purchaseRequestId);

        List<PurchaseRequestDecisionResponse> response =
                decisions.stream()
                        .map(this::toDecisionResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    private PurchaseRequestResponse toResponse(
            PurchaseRequest purchaseRequest) {

        List<PurchaseRequestLine> lines =
                purchaseRequestLineRepo.findByPurchaseRequest_SysId(
                        purchaseRequest.getSysId());

        return purchaseRequestTransformer.toResponse(
                purchaseRequest,
                lines);
    }

    private PurchaseRequestDecisionResponse toDecisionResponse(
            PurchaseRequestDecision decision) {

        String actorName = null;

        if (decision.getActor() != null) {
            actorName =
                    decision.getActor().getFirstName() + " " +
                            decision.getActor().getLastName();
        }

        return PurchaseRequestDecisionResponse.builder()
                .sysId(decision.getSysId())
                .actorEmployeeSysId(
                        decision.getActor() != null
                                ? decision.getActor().getSysId()
                                : null)
                .actorName(actorName)
                .decisionType(decision.getDecisionType())
                .fromStatus(decision.getFromStatus())
                .toStatus(decision.getToStatus())
                .reason(decision.getReason())
                .decisionAt(decision.getDecisionAt())
                .build();
    }

    @PatchMapping("/{purchaseRequestId}")
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<PurchaseRequestResponse> updateDraftPurchaseRequest(
            @PathVariable UUID purchaseRequestId,
            @Valid @RequestBody PurchaseRequestUpdateRequest request) {

        PurchaseRequest purchaseRequest =
                purchaseRequestService.updateDraftPurchaseRequest(
                        purchaseRequestId,
                        request);

        return ResponseEntity.ok(toResponse(purchaseRequest));
    }
}
