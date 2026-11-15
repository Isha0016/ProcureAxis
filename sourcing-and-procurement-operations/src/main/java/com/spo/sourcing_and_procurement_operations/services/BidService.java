package com.spo.sourcing_and_procurement_operations.services;

import com.fsc.supply_chain_common_operations.enums.*;
import com.fsc.supply_chain_common_operations.models.*;
import com.fsc.supply_chain_common_operations.repositories.*;

import com.spo.sourcing_and_procurement_operations.dtos.requests.*;
import com.spo.sourcing_and_procurement_operations.repositories.SourcingRequestLineRepo;
import com.spo.sourcing_and_procurement_operations.repositories.SourcingRequestRepo;
import com.spo.sourcing_and_procurement_operations.repositories.SourcingSupplierInvitationRepo;

import io.jsonwebtoken.Claims;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class BidService {

    private final EmployeeRepo employeeRepo;
    private final SourcingRequestRepo sourcingRequestRepo;
    private final SourcingRequestLineRepo sourcingRequestLineRepo;
    private final SourcingSupplierInvitationRepo invitationRepo;
    private final BidRepo bidRepo;
    private final BidLineRepo bidLineRepo;
    private final SourcingAwardRepo awardRepo;

    public BidService(
            EmployeeRepo employeeRepo,
            SourcingRequestRepo sourcingRequestRepo,
            SourcingRequestLineRepo sourcingRequestLineRepo,
            SourcingSupplierInvitationRepo invitationRepo,
            BidRepo bidRepo,
            BidLineRepo bidLineRepo,
            SourcingAwardRepo awardRepo) {

        this.employeeRepo = employeeRepo;
        this.sourcingRequestRepo = sourcingRequestRepo;
        this.sourcingRequestLineRepo = sourcingRequestLineRepo;
        this.invitationRepo = invitationRepo;
        this.bidRepo = bidRepo;
        this.bidLineRepo = bidLineRepo;
        this.awardRepo = awardRepo;
    }

    // 1. CREATE BID

    @Transactional
    public Bid createDraft(
            UUID sourcingRequestId,
            BidCreateRequest request) {

        Employee actor = authenticatedEmployee();

        requireActiveSupplier(actor);
        requireOperation(actor, "CREATE_BID");

        SourcingRequest sourcing =
                getSourcingRequest(sourcingRequestId);

        requirePublishedAndOpen(sourcing);

        requireInvitation(
                sourcingRequestId,
                actor.getCompany().getSysId()
        );

        if (!(actor.getCompany() instanceof SupplierCompany supplier)) {
            throw new AccessDeniedException(
                    "Authenticated company is not a supplier");
        }

        if (bidRepo.existsBySourcingRequest_SysIdAndSupplierCompany_SysId(
                sourcingRequestId, supplier.getSysId())) {

            throw new IllegalStateException(
                    "Your company has already created a bid for this sourcing request");
        }

        Bid bid = new Bid();

        bid.setBidNumber(
                "BID-" + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase(Locale.ROOT)
        );

        bid.setSourcingRequest(sourcing);
        bid.setSupplierCompany(supplier);
        bid.setBidStatus(BidStatus.DRAFT);
        bid.setCurrencyCode(sourcing.getCurrencyCode());

        applyDraftFields(bid, request, sourcing);

        bid = bidRepo.save(bid);

        saveLines(bid, sourcing, request.getLines());

        recalculateTotal(bid);

        return bidRepo.save(bid);
    }

    // 2. UPDATE DRAFT BID

    @Transactional
    public Bid updateDraft(
            UUID bidId,
            BidCreateRequest request) {

        Employee actor = authenticatedEmployee();

        requireActiveSupplier(actor);
        requireOperation(actor, "UPDATE_BID");

        Bid bid = getBidForUpdate(bidId);

        requireSupplierOwner(bid, actor);

        if (bid.getBidStatus() != BidStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT bids can be edited");
        }

        requireExpectedBidVersion(
                bid,
                request.getExpectedVersion()
        );

        SourcingRequest sourcing = bid.getSourcingRequest();

        requirePublishedAndOpen(sourcing);

        applyDraftFields(bid, request, sourcing);

        bidLineRepo.deleteByBid_SysId(bidId);

        saveLines(bid, sourcing, request.getLines());

        recalculateTotal(bid);

        return bidRepo.save(bid);
    }

    // 3. SUBMIT BID

    @Transactional
    public Bid submit(UUID bidId, Long expectedVersion) {

        Employee actor = authenticatedEmployee();

        requireActiveSupplier(actor);
        requireOperation(actor, "SUBMIT_BID");

        Bid bid = getBidForUpdate(bidId);

        requireSupplierOwner(bid, actor);

        if (bid.getBidStatus() != BidStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT bids can be submitted");
        }

        requireExpectedBidVersion(bid, expectedVersion);

        SourcingRequest sourcing = bid.getSourcingRequest();

        requirePublishedAndOpen(sourcing);

        List<SourcingRequestLine> requiredLines =
                sourcingRequestLineRepo
                        .findBySourcingRequest_SysIdOrderByLineNumberAsc(
                                sourcing.getSysId());

        List<BidLine> bidLines =
                bidLineRepo.findByBid_SysIdOrderByCreatedAtAsc(
                        bidId);

        if (requiredLines.isEmpty()
                || bidLines.size() != requiredLines.size()) {

            throw new IllegalArgumentException(
                    "A bid must contain exactly one quote for every sourcing line");
        }

        Set<UUID> quotedLineIds = new HashSet<>();

        for (BidLine line : bidLines) {

            if (!quotedLineIds.add(
                    line.getSourcingRequestLine().getSysId())) {

                throw new IllegalArgumentException(
                        "Duplicate sourcing line in bid");
            }

            if (line.getQuotedQuantity() == null
                    || line.getQuotedQuantity().signum() <= 0) {

                throw new IllegalArgumentException(
                        "Quoted quantity must be greater than zero");
            }

            if (line.getQuotedUnitPrice() == null
                    || line.getQuotedUnitPrice().signum() < 0) {

                throw new IllegalArgumentException(
                        "Quoted unit price cannot be negative");
            }

            if (!Objects.equals(
                    line.getCurrencyCode(),
                    sourcing.getCurrencyCode())) {

                throw new IllegalArgumentException(
                        "Bid line currency must match sourcing currency");
            }
        }

        for (SourcingRequestLine required : requiredLines) {

            if (!quotedLineIds.contains(required.getSysId())) {

                throw new IllegalArgumentException(
                        "Missing bid quote for sourcing line "
                                + required.getLineNumber());
            }
        }

        if (bid.getValidUntil() == null
                || bid.getValidUntil().isBefore(
                sourcing.getResponseDeadline()
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate())) {

            throw new IllegalArgumentException(
                    "Bid validity must extend through the response deadline");
        }

        recalculateTotal(bid);

        bid.setBidStatus(BidStatus.SUBMITTED);
        bid.setSubmissionDate(Instant.now());

        return bidRepo.save(bid);
    }

    // 4. WITHDRAW BID

    @Transactional
    public Bid withdraw(UUID bidId, Long expectedVersion) {

        Employee actor = authenticatedEmployee();

        requireActiveSupplier(actor);
        requireOperation(actor, "WITHDRAW_BID");

        Bid bid = getBidForUpdate(bidId);

        requireSupplierOwner(bid, actor);

        if (bid.getBidStatus() != BidStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Only a SUBMITTED bid can be withdrawn");
        }

        requireExpectedBidVersion(bid, expectedVersion);

        Instant deadline =
                bid.getSourcingRequest().getResponseDeadline();

        if (deadline == null
                || !Instant.now().isBefore(deadline)) {

            throw new IllegalStateException(
                    "A bid can only be withdrawn before the response deadline");
        }

        bid.setBidStatus(BidStatus.WITHDRAWN);

        return bidRepo.save(bid);
    }

    // 5. GET BID

    @Transactional(readOnly = true)
    public Bid getBid(UUID bidId) {

        Employee actor = authenticatedEmployee();

        Bid bid = bidRepo.findById(bidId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Bid not found"));

        if (isSupplier(actor)) {

            requireActiveSupplier(actor);
            requireSupplierOwner(bid, actor);
            requireOperation(actor, "VIEW_BID");

            return bid;
        }

        requireBuyerOwner(
                bid.getSourcingRequest(),
                actor
        );

        requireOperation(actor, "VIEW_BID");

        requireResponseDeadlinePassed(
                bid.getSourcingRequest()
        );

        return bid;
    }

    // 6. LIST BIDS

    @Transactional(readOnly = true)
    public List<Bid> listForSourcingRequest(
            UUID sourcingRequestId) {

        Employee actor = authenticatedEmployee();

        SourcingRequest sourcing =
                getSourcingRequest(sourcingRequestId);

        if (isSupplier(actor)) {

            requireActiveSupplier(actor);
            requireOperation(actor, "VIEW_BID");

            requireInvitation(
                    sourcingRequestId,
                    actor.getCompany().getSysId()
            );

            return bidRepo
                    .findBySourcingRequest_SysIdAndSupplierCompany_SysId(
                            sourcingRequestId,
                            actor.getCompany().getSysId())
                    .map(List::of)
                    .orElseGet(List::of);
        }

        requireBuyerOwner(sourcing, actor);
        requireOperation(actor, "VIEW_BID");

        requireResponseDeadlinePassed(sourcing);

        return bidRepo
                .findBySourcingRequest_SysIdOrderByCreatedAtAsc(
                        sourcingRequestId);
    }

    // 7. COMPARE BIDS

    @Transactional
    public List<Bid> compare(UUID sourcingRequestId) {

        Employee actor = authenticatedEmployee();

        SourcingRequest sourcing =
                getSourcingRequest(sourcingRequestId);

        requireBuyerOwner(sourcing, actor);

        requireOperation(actor, "COMPARE_BIDS");

        requireStatus(
                sourcing,
                RequestStatus.IN_PROGRESS
        );

        requireResponseDeadlinePassed(sourcing);

        if (sourcing.getEvaluationDeadline() != null
                && Instant.now().isAfter(
                sourcing.getEvaluationDeadline())) {

            throw new IllegalStateException(
                    "The evaluation window for this sourcing request has closed");
        }

        List<Bid> bids =
                bidRepo.findBySourcingRequest_SysIdOrderByCreatedAtAsc(
                        sourcingRequestId);

        for (Bid bid : bids) {

            if (bid.getBidStatus() == BidStatus.SUBMITTED) {

                if (bid.getValidUntil() != null
                        && !bid.getValidUntil().isBefore(LocalDate.now())) {

                    bid.setBidStatus(BidStatus.UNDER_EVALUATION);
                    bid.setEvaluatedAt(Instant.now());

                } else {

                    bid.setBidStatus(BidStatus.EXPIRED);
                }

                bidRepo.save(bid);
            }
        }

        return bidRepo
                .findBySourcingRequest_SysIdOrderByCreatedAtAsc(
                        sourcingRequestId)
                .stream()
                .filter(b ->
                        b.getBidStatus() == BidStatus.UNDER_EVALUATION
                                || b.getBidStatus() == BidStatus.SUBMITTED)
                .toList();
    }

    // 8. AWARD BID

    @Transactional
    public SourcingAward award(
            UUID sourcingRequestId,
            BidAwardRequest request) {

        Employee actor = authenticatedEmployee();

        requireActiveBuyer(actor);
        requireOperation(actor, "AWARD_BID");

        SourcingRequest sourcing =
                sourcingRequestRepo.findByIdForUpdate(
                                sourcingRequestId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Sourcing request not found"));

        requireBuyerOwner(sourcing, actor);

        requireStatus(
                sourcing,
                RequestStatus.IN_PROGRESS
        );

        if (sourcing.getVersion() == null
                || !sourcing.getVersion().equals(
                request.getExpectedSourcingVersion())) {

            throw new IllegalStateException(
                    "Sourcing request was changed. Refresh and retry the award");
        }

        requireResponseDeadlinePassed(sourcing);

        if (sourcing.getEvaluationDeadline() != null
                && Instant.now().isAfter(
                sourcing.getEvaluationDeadline())) {

            throw new IllegalStateException(
                    "The evaluation window for this sourcing request has closed");
        }

        if (awardRepo.existsBySourcingRequest_SysId(
                sourcingRequestId)) {

            throw new IllegalStateException(
                    "An award has already been recorded for this sourcing request");
        }

        Bid winningBid = getBidForUpdate(
                request.getBidSysId());

        if (!winningBid.getSourcingRequest()
                .getSysId().equals(sourcingRequestId)) {

            throw new IllegalArgumentException(
                    "Selected bid does not belong to this sourcing request");
        }

        requireInvitation(
                sourcingRequestId,
                winningBid.getSupplierCompany().getSysId()
        );

        if (winningBid.getSupplierCompany().getStatus()
                != CompanyStatus.ACTIVE) {

            throw new IllegalStateException(
                    "The selected supplier company is not ACTIVE");
        }

        if (winningBid.getBidStatus() != BidStatus.SUBMITTED
                && winningBid.getBidStatus()
                != BidStatus.UNDER_EVALUATION) {

            throw new IllegalStateException(
                    "Only an eligible submitted bid can be awarded");
        }

        if (winningBid.getValidUntil() == null
                || winningBid.getValidUntil()
                .isBefore(LocalDate.now())) {

            throw new IllegalStateException(
                    "Selected bid has expired");
        }

        winningBid.setBidStatus(BidStatus.ACCEPTED);
        winningBid.setEvaluatedAt(Instant.now());

        bidRepo.save(winningBid);

        List<Bid> allBids =
                bidRepo.findBySourcingRequest_SysIdOrderByCreatedAtAsc(
                        sourcingRequestId);

        for (Bid other : allBids) {

            if (!other.getSysId().equals(winningBid.getSysId())
                    && (other.getBidStatus() == BidStatus.SUBMITTED
                    || other.getBidStatus()
                    == BidStatus.UNDER_EVALUATION)) {

                other.setBidStatus(BidStatus.REJECTED);
                other.setEvaluatedAt(Instant.now());

                bidRepo.save(other);
            }
        }

        SourcingAward award = new SourcingAward();

        award.setSourcingRequest(sourcing);
        award.setAcceptedBid(winningBid);
        award.setAwardedBy(actor);
        award.setReason(request.getReason().trim());
        award.setAwardedAt(Instant.now());

        award = awardRepo.save(award);

        sourcing.setClosureOutcome(
                SourcingClosureOutcome.AWARDED
        );

        sourcing.setClosureReason(
                request.getReason().trim()
        );

        sourcing.setClosedAt(Instant.now());

        sourcing.setRequestStatus(
                RequestStatus.COMPLETED
        );

        sourcingRequestRepo.save(sourcing);

        return award;
    }

    // 9. GET AWARD

    @Transactional(readOnly = true)
    public Optional<SourcingAward> getAward(
            UUID sourcingRequestId) {

        Employee actor = authenticatedEmployee();

        SourcingRequest sourcing =
                getSourcingRequest(sourcingRequestId);

        requireBuyerOwner(sourcing, actor);
        requireOperation(actor, "VIEW_BID");

        return awardRepo.findBySourcingRequest_SysId(
                sourcingRequestId);
    }

    // 10. VALIDATION METHODS

    private void requireExpectedBidVersion(
            Bid bid,
            Long expectedVersion) {

        if (expectedVersion == null
                || bid.getVersion() == null
                || !bid.getVersion().equals(expectedVersion)) {

            throw new IllegalStateException(
                    "Bid was changed. Refresh the bid and retry");
        }
    }

    private void applyDraftFields(
            Bid bid,
            BidCreateRequest request,
            SourcingRequest sourcing) {

        if (request.getValidUntil() == null
                || request.getValidUntil()
                .isBefore(LocalDate.now())) {

            throw new IllegalArgumentException(
                    "Bid valid-until date must be today or in the future");
        }

        if (request.getDeliveryLeadTimeDays() == null
                || request.getDeliveryLeadTimeDays() < 1) {

            throw new IllegalArgumentException(
                    "Delivery lead time must be at least one day");
        }

        if (request.getLines() == null
                || request.getLines().isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one bid line is required");
        }

        bid.setValidUntil(request.getValidUntil());
        bid.setDeliveryLeadTimeDays(
                request.getDeliveryLeadTimeDays());

        bid.setPaymentTerms(request.getPaymentTerms());
        bid.setDeliveryTerms(request.getDeliveryTerms());
        bid.setTechnicalProposal(request.getTechnicalProposal());
        bid.setCommercialProposal(request.getCommercialProposal());
        bid.setRemarks(request.getRemarks());

        bid.setCurrencyCode(sourcing.getCurrencyCode());
    }

    private void saveLines(
            Bid bid,
            SourcingRequest sourcing,
            List<BidLineRequest> lineRequests) {

        List<SourcingRequestLine> required =
                sourcingRequestLineRepo
                        .findBySourcingRequest_SysIdOrderByLineNumberAsc(
                                sourcing.getSysId());

        Map<UUID, SourcingRequestLine> byId = new HashMap<>();

        required.forEach(
                line -> byId.put(line.getSysId(), line)
        );

        Set<UUID> seen = new HashSet<>();

        List<BidLine> toSave = new ArrayList<>();

        for (BidLineRequest lineRequest : lineRequests) {

            if (!seen.add(
                    lineRequest.getSourcingRequestLineSysId())) {

                throw new IllegalArgumentException(
                        "Duplicate sourcing request line in bid");
            }

            SourcingRequestLine sourceLine =
                    byId.get(
                            lineRequest.getSourcingRequestLineSysId());

            if (sourceLine == null) {

                throw new IllegalArgumentException(
                        "Bid line does not belong to this sourcing request");
            }

            BigDecimal qty = lineRequest.getQuotedQuantity();
            BigDecimal price = lineRequest.getQuotedUnitPrice();

            if (qty == null || qty.signum() <= 0) {
                throw new IllegalArgumentException(
                        "Quoted quantity must be greater than zero");
            }

            if (price == null || price.signum() < 0) {
                throw new IllegalArgumentException(
                        "Quoted unit price cannot be negative");
            }

            BidLine bidLine = new BidLine();

            bidLine.setBid(bid);
            bidLine.setSourcingRequestLine(sourceLine);

            bidLine.setItemDescriptionSnapshot(
                    sourceLine.getItemDescription());

            bidLine.setQuotedQuantity(qty);
            bidLine.setQuotedUnitPrice(price);

            bidLine.setLineAmount(
                    qty.multiply(price)
                            .setScale(4, RoundingMode.HALF_UP)
            );

            bidLine.setCurrencyCode(
                    sourcing.getCurrencyCode());

            bidLine.setRemarks(lineRequest.getRemarks());

            toSave.add(bidLine);
        }

        bidLineRepo.saveAll(toSave);
    }

    private void recalculateTotal(Bid bid) {

        BigDecimal total = bidLineRepo
                .findByBid_SysIdOrderByCreatedAtAsc(
                        bid.getSysId())
                .stream()
                .map(BidLine::getLineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        bid.setTotalBidAmount(
                total.setScale(4, RoundingMode.HALF_UP)
        );
    }

    // 11. AUTHENTICATION

    private Employee authenticatedEmployee() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof Claims claims)) {

            throw new AccessDeniedException("Invalid authentication");
        }

        String email = claims.get("email", String.class);

        if (email == null || email.isBlank()) {
            throw new AccessDeniedException("Employee email missing from token");
        }

        Employee employee = employeeRepo.findByWorkEmail(email);

        if (employee == null) {
            throw new AccessDeniedException("Employee not found");
        }

        if (employee.getCompany() == null) {
            throw new AccessDeniedException("Employee company not found");
        }

        if (employee.getUserStatus() != UserStatus.ACTIVE ||
                employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {

            throw new AccessDeniedException("Employee account is not active");
        }

        String companyId = claims.get("companyId", String.class);

        if (companyId == null ||
                !employee.getCompany().getSysId().toString().equals(companyId)) {

            throw new AccessDeniedException("Company mismatch");
        }

        return employee;
    }

    private boolean isSupplier(Employee employee) {

        return employee.getCompany() != null
                && employee.getCompany().getCompanyType()
                == CompanyType.SUPPLIER_COMPANY;
    }

    private void requireActiveSupplier(Employee employee) {

        if (!isSupplier(employee)
                || employee.getCompany().getStatus()
                != CompanyStatus.ACTIVE) {

            throw new AccessDeniedException(
                    "Only employees of an ACTIVE supplier company can perform this action");
        }
    }

    private void requireActiveBuyer(Employee employee) {

        if (employee.getCompany() == null
                || employee.getCompany().getCompanyType()
                != CompanyType.PROCUREMENT_COMPANY
                || employee.getCompany().getStatus()
                != CompanyStatus.ACTIVE) {

            throw new AccessDeniedException(
                    "Only employees of an ACTIVE procurement company can perform this action");
        }
    }

    private void requireBuyerOwner(
            SourcingRequest sourcing,
            Employee actor) {

        requireActiveBuyer(actor);

        if (sourcing.getCompany() == null
                || !sourcing.getCompany().getSysId()
                .equals(actor.getCompany().getSysId())) {

            throw new AccessDeniedException(
                    "You cannot access another company's sourcing request");
        }
    }

    private void requireSupplierOwner(
            Bid bid,
            Employee actor) {

        if (bid.getSupplierCompany() == null
                || !bid.getSupplierCompany().getSysId()
                .equals(actor.getCompany().getSysId())) {

            throw new AccessDeniedException(
                    "You cannot access another supplier's bid");
        }
    }

    private void requireOperation(
            Employee employee,
            String operationName) {

        boolean allowed = employee.getRoles() != null
                && employee.getRoles().stream()
                .filter(role -> role.getOperations() != null)
                .flatMap(role -> role.getOperations().stream())
                .map(Operation::getOperationName)
                .anyMatch(operationName::equals);

        if (!allowed) {

            throw new AccessDeniedException(
                    "You do not have permission: " + operationName);
        }
    }

    private void requireInvitation(
            UUID sourcingId,
            UUID supplierId) {

        invitationRepo
                .findBySourcingRequest_SysIdAndSupplierCompany_SysIdAndStatus(
                        sourcingId,
                        supplierId,
                        SourcingInvitationStatus.INVITED)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Your company is not invited to this sourcing request"));
    }

    private void requirePublishedAndOpen(
            SourcingRequest sourcing) {

        if (sourcing.getRequestStatus()
                != RequestStatus.IN_PROGRESS
                || sourcing.getPublishedAt() == null) {

            throw new IllegalStateException(
                    "Bids can only be created for published sourcing requests");
        }

        if (sourcing.getResponseDeadline() == null
                || !Instant.now().isBefore(
                sourcing.getResponseDeadline())) {

            throw new IllegalStateException(
                    "The bid response deadline has passed");
        }
    }

    private void requireResponseDeadlinePassed(
            SourcingRequest sourcing) {

        if (sourcing.getResponseDeadline() == null
                || Instant.now().isBefore(
                sourcing.getResponseDeadline())) {

            throw new AccessDeniedException(
                    "Bid comparison is available only after the response deadline");
        }
    }

    private SourcingRequest getSourcingRequest(UUID id) {

        return sourcingRequestRepo.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Sourcing request not found"));
    }

    private Bid getBidForUpdate(UUID id) {

        return bidRepo.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Bid not found"));
    }

    private void requireStatus(
            SourcingRequest sourcing,
            RequestStatus expected) {

        if (sourcing.getRequestStatus() != expected) {

            throw new IllegalStateException(
                    "Sourcing request must be in " + expected + " state");
        }
    }
}
