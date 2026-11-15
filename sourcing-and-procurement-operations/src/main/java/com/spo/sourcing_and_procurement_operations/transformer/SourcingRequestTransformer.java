package com.spo.sourcing_and_procurement_operations.transformer;

import com.fsc.supply_chain_common_operations.models.SourcingRequest;
import com.fsc.supply_chain_common_operations.models.SourcingRequestLine;
import com.fsc.supply_chain_common_operations.models.SourcingSupplierInvitation;
import com.spo.sourcing_and_procurement_operations.dtos.responses.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SourcingRequestTransformer {

    public SourcingRequestResponse toResponse(
            SourcingRequest request,
            List<SourcingRequestLine> lines,
            List<SourcingSupplierInvitation> invitations) {

        return SourcingRequestResponse.builder()
                .sysId(request.getSysId())
                .requestNumber(request.getRequestNumber())
                .title(request.getTitle())
                .description(request.getDescription())

                .sourcePurchaseRequestSysId(
                        request.getSourcePurchaseRequest() == null
                                ? null
                                : request.getSourcePurchaseRequest().getSysId())

                .sourcePurchaseRequestNumber(
                        request.getSourcePurchaseRequest() == null
                                ? null
                                : request.getSourcePurchaseRequest().getRequestNumber())

                .companySysId(
                        request.getCompany() == null
                                ? null
                                : request.getCompany().getSysId())

                .requestStatus(request.getRequestStatus())
                .priority(request.getPriority())
                .sourcingMethod(request.getSourcingMethod())
                .currencyCode(request.getCurrencyCode())
                .estimatedTotalAmount(request.getEstimatedTotalAmount())
                .deliveryLocation(request.getDeliveryLocation())
                .budgetCode(request.getBudgetCode())
                .procurementType(request.getProcurementType())
                .responseDeadline(request.getResponseDeadline())
                .evaluationDeadline(request.getEvaluationDeadline())
                .publishedAt(request.getPublishedAt())
                .closedAt(request.getClosedAt())
                .closureReason(request.getClosureReason())
                .closureOutcome(request.getClosureOutcome())
                .evaluationCriteria(request.getEvaluationCriteria())
                .commercialTerms(request.getCommercialTerms())
                .submitterEmployeeSysId(
                        request.getSubmitterEmployee() == null
                                ? null
                                : request.getSubmitterEmployee().getSysId())

                .procurementApprovalManagerSysId(
                        request.getProcurementApprovalManager() == null
                                ? null
                                : request.getProcurementApprovalManager().getSysId())

                .version(request.getVersion())
                .targetSupplierCount(request.getTargetSupplierCount())
                .requiredDate(request.getRequiredDate())

                .lines(lines.stream()
                        .map(this::toLineResponse)
                        .toList())

                .invitedSuppliers(invitations.stream()
                        .map(this::toInvitationResponse)
                        .toList())

                .build();
    }

    private SourcingRequestLineResponse toLineResponse(
            SourcingRequestLine line) {

        return SourcingRequestLineResponse.builder()
                .sysId(line.getSysId())
                .lineNumber(line.getLineNumber())
                .sourceProductSysId(line.getSourceProductSysId())
                .productCode(line.getProductCodeSnapshot())
                .itemDescription(line.getItemDescription())
                .quantity(line.getQuantity())
                .unitOfMeasure(line.getUnitOfMeasure())
                .notes(line.getNotes())
                .estimatedUnitPrice(line.getEstimatedUnitPrice())
                .estimatedLineAmount(line.getEstimatedLineAmount())
                .currencyCode(line.getCurrencyCode())
                .requiredDate(line.getRequiredDate())
                .deliveryLocation(line.getDeliveryLocation())
                .taxRate(line.getTaxRate())
                .build();
    }

    private SourcingSupplierInvitationResponse toInvitationResponse(
            SourcingSupplierInvitation invitation) {

        return SourcingSupplierInvitationResponse.builder()
                .supplierCompanySysId(
                        invitation.getSupplierCompany().getSysId())
                .supplierCompanyName(
                        invitation.getSupplierCompany().getCompanyName())
                .status(invitation.getStatus())
                .invitedAt(invitation.getInvitedAt())
                .build();
    }
}