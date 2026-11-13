package com.spo.sourcing_and_procurement_operations.transformer;

import com.fsc.supply_chain_common_operations.models.PurchaseRequest;
import com.fsc.supply_chain_common_operations.models.PurchaseRequestLine;
import com.spo.sourcing_and_procurement_operations.dtos.responses.PurchaseRequestResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PurchaseRequestTransformer {

    private final PurchaseRequestLineTransformer lineTransformer;

    public PurchaseRequestTransformer(
            PurchaseRequestLineTransformer lineTransformer) {
        this.lineTransformer = lineTransformer;
    }

    public PurchaseRequestResponse toResponse(
            PurchaseRequest purchaseRequest,
            List<PurchaseRequestLine> requestLines) {

        return PurchaseRequestResponse.builder()
                .requestLines(requestLines.stream()
                                .map(lineTransformer::toResponse)
                                .toList()
                )
                .sysId(purchaseRequest.getSysId())
                .requestNumber(purchaseRequest.getRequestNumber())
                .title(purchaseRequest.getTitle())
                .description(purchaseRequest.getDescription())
                .requestStatus(purchaseRequest.getRequestStatus())
                .priority(purchaseRequest.getPriority())
                .requiredDate(purchaseRequest.getRequiredDate())

                .companySysId(
                        purchaseRequest.getCompany() != null
                                ? purchaseRequest.getCompany().getSysId()
                                : null)

                .companyName(
                        purchaseRequest.getCompany() != null
                                ? purchaseRequest.getCompany().getCompanyName()
                                : null)

                .submitterEmployeeSysId(
                        purchaseRequest.getSubmitterEmployee() != null
                                ? purchaseRequest.getSubmitterEmployee().getSysId()
                                : null)

                .deliveryLocation(purchaseRequest.getDeliveryLocation())
                .currencyCode(purchaseRequest.getCurrencyCode())
                .budgetCode(purchaseRequest.getBudgetCode())
                .build();
    }
}
