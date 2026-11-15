package com.spo.sourcing_and_procurement_operations.transformer;

import com.fsc.supply_chain_common_operations.models.Bid;
import com.fsc.supply_chain_common_operations.models.BidLine;

import com.spo.sourcing_and_procurement_operations.dtos.responses.BidLineResponse;
import com.spo.sourcing_and_procurement_operations.dtos.responses.BidResponse;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BidTransformer {

    public BidResponse toResponse(Bid bid, List<BidLine> lines) {

        return BidResponse.builder()
                .sysId(bid.getSysId())
                .bidNumber(bid.getBidNumber())
                .sourcingRequestSysId(
                        bid.getSourcingRequest().getSysId())
                .supplierCompanySysId(
                        bid.getSupplierCompany().getSysId())
                .supplierCompanyName(
                        bid.getSupplierCompany().getCompanyName())
                .bidStatus(bid.getBidStatus())
                .submissionDate(bid.getSubmissionDate())
                .validUntil(bid.getValidUntil())
                .currencyCode(bid.getCurrencyCode())
                .totalBidAmount(bid.getTotalBidAmount())
                .deliveryLeadTimeDays(bid.getDeliveryLeadTimeDays())
                .paymentTerms(bid.getPaymentTerms())
                .deliveryTerms(bid.getDeliveryTerms())
                .technicalProposal(bid.getTechnicalProposal())
                .commercialProposal(bid.getCommercialProposal())
                .remarks(bid.getRemarks())
                .version(bid.getVersion())
                .lines(
                        lines.stream()
                                .map(this::toLineResponse)
                                .toList()
                )
                .build();
    }

    private BidLineResponse toLineResponse(BidLine line) {

        return BidLineResponse.builder()
                .sysId(line.getSysId())
                .sourcingRequestLineSysId(
                        line.getSourcingRequestLine().getSysId())
                .sourcingLineNumber(
                        line.getSourcingRequestLine().getLineNumber())
                .itemDescription(
                        line.getItemDescriptionSnapshot())
                .requiredQuantity(
                        line.getSourcingRequestLine().getQuantity())
                .quotedQuantity(line.getQuotedQuantity())
                .quotedUnitPrice(line.getQuotedUnitPrice())
                .lineAmount(line.getLineAmount())
                .currencyCode(line.getCurrencyCode())
                .build();
    }
}
