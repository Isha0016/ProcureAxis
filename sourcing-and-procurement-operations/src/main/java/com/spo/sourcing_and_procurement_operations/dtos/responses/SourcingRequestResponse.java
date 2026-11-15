package com.spo.sourcing_and_procurement_operations.dtos.responses;

import com.fsc.supply_chain_common_operations.enums.ProcurementType;
import com.fsc.supply_chain_common_operations.enums.RequestPriority;
import com.fsc.supply_chain_common_operations.enums.RequestStatus;
import com.fsc.supply_chain_common_operations.enums.SourcingMethod;
import com.fsc.supply_chain_common_operations.enums.SourcingClosureOutcome;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SourcingRequestResponse {

    private UUID sysId;

    private String requestNumber;

    private UUID sourcePurchaseRequestSysId;

    private String title;

    private String description;

    private RequestStatus requestStatus;

    private RequestPriority priority;

    private SourcingMethod sourcingMethod;

    private UUID companySysId;

    private String companyName;

    private UUID submitterEmployeeSysId;

    private UUID procurementApprovalManagerSysId;

    private String currencyCode;

    private LocalDate requiredDate;

    private Instant responseDeadline;

    private Instant evaluationDeadline;

    private Instant publishedAt;

    private Instant closedAt;

    private String evaluationCriteria;

    private String commercialTerms;

    private Integer targetSupplierCount;

    private Long version;

    private List<SourcingRequestLineResponse> lines;

    private String sourcePurchaseRequestNumber;

    private String closureReason;

    private List<SourcingSupplierInvitationResponse> invitedSuppliers;

    private BigDecimal estimatedTotalAmount;

    private String deliveryLocation;

    private String budgetCode;

    private ProcurementType procurementType;

    private SourcingClosureOutcome closureOutcome;
}
