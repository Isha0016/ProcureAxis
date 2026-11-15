package com.spo.sourcing_and_procurement_operations.dtos.responses;

import com.fsc.supply_chain_common_operations.enums.BidStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BidResponse {

    private UUID sysId;

    private String bidNumber;

    private UUID sourcingRequestSysId;

    private UUID supplierCompanySysId;

    private String supplierCompanyName;

    private BidStatus bidStatus;

    private Instant submissionDate;

    private LocalDate validUntil;

    private String currencyCode;

    private BigDecimal totalBidAmount;

    private Integer deliveryLeadTimeDays;

    private String paymentTerms;

    private String deliveryTerms;

    private String technicalProposal;

    private String commercialProposal;

    private String remarks;

    private Long version;

    private List<BidLineResponse> lines;
}
