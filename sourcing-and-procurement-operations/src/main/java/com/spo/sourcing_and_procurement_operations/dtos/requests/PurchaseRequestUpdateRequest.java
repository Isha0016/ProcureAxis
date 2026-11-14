package com.spo.sourcing_and_procurement_operations.dtos.requests;

import com.fsc.supply_chain_common_operations.enums.ProcurementType;
import com.fsc.supply_chain_common_operations.enums.PurchaseRequestType;
import com.fsc.supply_chain_common_operations.enums.RequestPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseRequestUpdateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotNull(message = "Priority is required")
    private RequestPriority priority;

    private LocalDate requiredDate;

    private String deliveryLocation;

    private String currencyCode;

    private String budgetCode;

    private PurchaseRequestType purchaseRequestType;

    private ProcurementType procurementType;

    @NotEmpty(message = "At least one request line is required")
    @Valid
    private List<PurchaseRequestLineRequest> requestLines;
}