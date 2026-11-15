package com.spo.sourcing_and_procurement_operations.Controllers;

import com.fsc.supply_chain_common_operations.models.Bid;
import com.fsc.supply_chain_common_operations.models.BidLine;
import com.fsc.supply_chain_common_operations.models.SourcingAward;
import com.fsc.supply_chain_common_operations.repositories.BidLineRepo;

import com.spo.sourcing_and_procurement_operations.dtos.requests.BidAwardRequest;
import com.spo.sourcing_and_procurement_operations.dtos.requests.BidActionRequest;
import com.spo.sourcing_and_procurement_operations.dtos.requests.BidCreateRequest;

import com.spo.sourcing_and_procurement_operations.dtos.responses.BidResponse;
import com.spo.sourcing_and_procurement_operations.dtos.responses.SourcingAwardResponse;

import com.spo.sourcing_and_procurement_operations.services.BidService;
import com.spo.sourcing_and_procurement_operations.transformer.BidTransformer;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/spo")
public class BidController {

    private final BidService bidService;
    private final BidLineRepo bidLineRepo;
    private final BidTransformer transformer;

    public BidController(
            BidService bidService,
            BidLineRepo bidLineRepo,
            BidTransformer transformer) {

        this.bidService = bidService;
        this.bidLineRepo = bidLineRepo;
        this.transformer = transformer;
    }

    // CREATE BID

    @PostMapping("/sourcing-requests/{sourcingRequestId}/bids")
    @PreAuthorize("@companyAccess.isSupplierCompany(authentication)")
    public ResponseEntity<BidResponse> createDraft(
            @PathVariable UUID sourcingRequestId,
            @Valid @RequestBody BidCreateRequest request) {

        Bid bid = bidService.createDraft(
                sourcingRequestId,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response(bid));
    }

    // LIST BIDS

    @GetMapping("/sourcing-requests/{sourcingRequestId}/bids")
    @PreAuthorize("@companyAccess.isSupplierCompany(authentication) or @companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<List<BidResponse>> listForSourcingRequest(
            @PathVariable UUID sourcingRequestId) {

        return ResponseEntity.ok(
                bidService.listForSourcingRequest(sourcingRequestId)
                        .stream()
                        .map(this::response)
                        .toList()
        );
    }

    // GET BID

    @GetMapping("/bids/{bidId}")
    @PreAuthorize("@companyAccess.isSupplierCompany(authentication) or @companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<BidResponse> getBid(
            @PathVariable UUID bidId) {

        return ResponseEntity.ok(
                response(bidService.getBid(bidId))
        );
    }

    // UPDATE DRAFT

    @PatchMapping("/bids/{bidId}")
    @PreAuthorize("@companyAccess.isSupplierCompany(authentication)")
    public ResponseEntity<BidResponse> updateDraft(
            @PathVariable UUID bidId,
            @Valid @RequestBody BidCreateRequest request) {

        return ResponseEntity.ok(
                response(
                        bidService.updateDraft(bidId, request)
                )
        );
    }

    // SUBMIT BID

    @PostMapping("/bids/{bidId}/submit")
    @PreAuthorize("@companyAccess.isSupplierCompany(authentication)")
    public ResponseEntity<BidResponse> submit(
            @PathVariable UUID bidId,
            @Valid @RequestBody BidActionRequest request) {

        return ResponseEntity.ok(
                response(
                        bidService.submit(
                                bidId,
                                request.getExpectedVersion()
                        )
                )
        );
    }

    // WITHDRAW BID

    @PostMapping("/bids/{bidId}/withdraw")
    @PreAuthorize("@companyAccess.isSupplierCompany(authentication)")
    public ResponseEntity<BidResponse> withdraw(
            @PathVariable UUID bidId,
            @Valid @RequestBody BidActionRequest request) {

        return ResponseEntity.ok(
                response(
                        bidService.withdraw(
                                bidId,
                                request.getExpectedVersion()
                        )
                )
        );
    }

    // COMPARE BIDS

    @GetMapping("/sourcing-requests/{sourcingRequestId}/comparison")
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<List<BidResponse>> compare(
            @PathVariable UUID sourcingRequestId) {

        return ResponseEntity.ok(
                bidService.compare(sourcingRequestId)
                        .stream()
                        .map(this::response)
                        .toList()
        );
    }

    // AWARD BID

    @PostMapping("/sourcing-requests/{sourcingRequestId}/award")
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<SourcingAwardResponse> award(
            @PathVariable UUID sourcingRequestId,
            @Valid @RequestBody BidAwardRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        toAwardResponse(
                                bidService.award(
                                        sourcingRequestId,
                                        request
                                )
                        )
                );
    }

    // GET AWARD

    @GetMapping("/sourcing-requests/{sourcingRequestId}/award")
    @PreAuthorize("@companyAccess.isProcurementCompany(authentication)")
    public ResponseEntity<SourcingAwardResponse> getAward(
            @PathVariable UUID sourcingRequestId) {

        return bidService.getAward(sourcingRequestId)
                .map(this::toAwardResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // BID RESPONSE TRANSFORMATION

    private BidResponse response(Bid bid) {

        List<BidLine> lines =
                bidLineRepo.findByBid_SysIdOrderByCreatedAtAsc(
                        bid.getSysId()
                );

        return transformer.toResponse(bid, lines);
    }

    // AWARD RESPONSE TRANSFORMATION

    private SourcingAwardResponse toAwardResponse(
            SourcingAward award) {

        return SourcingAwardResponse.builder()
                .sysId(award.getSysId())
                .sourcingRequestSysId(
                        award.getSourcingRequest().getSysId()
                )
                .acceptedBidSysId(
                        award.getAcceptedBid().getSysId()
                )
                .acceptedBidNumber(
                        award.getAcceptedBid().getBidNumber()
                )
                .awardedByEmployeeSysId(
                        award.getAwardedBy().getSysId()
                )
                .awardedByName(
                        award.getAwardedBy().getFirstName()
                                + " "
                                + award.getAwardedBy().getLastName()
                )
                .reason(award.getReason())
                .awardedAt(award.getAwardedAt())
                .build();
    }
}
