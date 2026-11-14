package com.spo.sourcing_and_procurement_operations.transformer;

import com.fsc.supply_chain_common_operations.models.PurchaseRequestLine;
import com.spo.sourcing_and_procurement_operations.dtos.responses.PurchaseRequestLineResponse;
import org.springframework.stereotype.Component;

@Component
public class PurchaseRequestLineTransformer {

    public PurchaseRequestLineResponse toResponse(
            PurchaseRequestLine requestLine) {

        return PurchaseRequestLineResponse.builder()
                .sysId(requestLine.getSysId())
                .lineNumber(requestLine.getLineNumber())
                .productId(
                        requestLine.getProduct() != null
                                ? requestLine.getProduct().getSysId()
                                : null)
                .productCode(
                        requestLine.getProduct() != null
                                ? requestLine.getProduct().getProductCode()
                                : null)
                .itemDescription(requestLine.getItemDescription())
                .quantity(requestLine.getQuantity())
                .unitOfMeasure(requestLine.getUnitOfMeasure())
                .estimatedUnitPrice(requestLine.getEstimatedUnitPrice())
                .estimatedLineAmount(requestLine.getEstimatedLineAmount())
                .currencyCode(requestLine.getCurrencyCode())
                .notes(requestLine.getNotes())
                .build();
    }
}