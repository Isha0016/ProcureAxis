package com.fsc.supply_chain_common_operations.models;

import com.fsc.supply_chain_common_operations.enums.UnitOfMeasure;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sourcing_request_lines")
public class SourcingRequestLine extends GlobalRecord {

    @ManyToOne(optional = false)
    @JoinColumn(name = "sourcing_request_sys_id", nullable = false)
    private SourcingRequest sourcingRequest;

    @ManyToOne(optional = false)
    @JoinColumn(name = "source_purchase_request_line_sys_id", nullable = false)
    private PurchaseRequestLine sourcePurchaseRequestLine;

    private UUID sourceProductSysId;

    @Column(length = 50)
    private String productCodeSnapshot;

    @Column(nullable = false)
    private Integer lineNumber;

    @Column(name = "item_description", nullable = false, length = 2000)
    private String itemDescription;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UnitOfMeasure unitOfMeasure;

    @Column(length = 1000)
    private String notes;

    @Column(precision = 19, scale = 4)
    private BigDecimal estimatedUnitPrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal estimatedLineAmount;

    @Column(length = 3)
    private String currencyCode;

    private LocalDate requiredDate;

    @Column(length = 500)
    private String deliveryLocation;

    @Column(precision = 7, scale = 4)
    private BigDecimal taxRate;
}