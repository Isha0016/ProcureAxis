package com.spo.sourcing_and_procurement_operations.Controllers;

import com.fsc.supply_chain_common_operations.models.PurchaseRequest;
import com.spo.sourcing_and_procurement_operations.dtos.requests.PurchaseRequestCreateRequest;
import com.spo.sourcing_and_procurement_operations.dtos.responses.PurchaseRequestResponse;
import com.spo.sourcing_and_procurement_operations.services.PurchaseRequestService;
import com.spo.sourcing_and_procurement_operations.transformer.PurchaseRequestTransformer;
import com.fsc.supply_chain_common_operations.models.PurchaseRequestLine;
import com.spo.sourcing_and_procurement_operations.repositories.PurchaseRequestLineRepo;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


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
        this.purchaseRequestTransformer=purchaseRequestTransformer;
        this.purchaseRequestLineRepo=purchaseRequestLineRepo;
    }

    @PostMapping
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<PurchaseRequestResponse> createPurchaseRequest(
            @Valid @RequestBody PurchaseRequestCreateRequest request) {

        PurchaseRequest purchaseRequest =
                purchaseRequestService.createPurchaseRequest(request);

        List<PurchaseRequestLine> lines =
                purchaseRequestLineRepo.findByPurchaseRequest_SysId(
                        purchaseRequest.getSysId());

        PurchaseRequestResponse response =
                purchaseRequestTransformer.toResponse(
                        purchaseRequest, lines);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{purchaseRequestId}")
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<PurchaseRequestResponse> getPurchaseRequest(
            @PathVariable UUID purchaseRequestId) {

        PurchaseRequest purchaseRequest =
                purchaseRequestService.getPurchaseRequest(purchaseRequestId);

        List<PurchaseRequestLine> lines =
                purchaseRequestLineRepo.findByPurchaseRequest_SysId(
                        purchaseRequest.getSysId());

        PurchaseRequestResponse response =
                purchaseRequestTransformer.toResponse(
                        purchaseRequest, lines);

        return ResponseEntity.ok(response);
    }
}
