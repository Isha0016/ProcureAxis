package com.spo.sourcing_and_procurement_operations.dtos.responses;

import com.fsc.supply_chain_common_operations.enums.RequestPriority;
import com.fsc.supply_chain_common_operations.enums.RequestStatus;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class PurchaseRequestResponse {
    private List<PurchaseRequestLineResponse> requestLines;
    private UUID sysId;

    private String requestNumber;

    private String title;

    private String description;

    private RequestStatus requestStatus;

    private RequestPriority priority;

    private LocalDate requiredDate;

    private UUID companySysId;

    private String companyName;

    private UUID submitterEmployeeSysId;

    private String deliveryLocation;

    private String currencyCode;

    private String budgetCode;
}
