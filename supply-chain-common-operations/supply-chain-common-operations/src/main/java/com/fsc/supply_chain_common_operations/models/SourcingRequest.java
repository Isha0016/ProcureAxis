package com.fsc.supply_chain_common_operations.models;

import com.fsc.supply_chain_common_operations.enums.ProcurementType;
import com.fsc.supply_chain_common_operations.enums.SourcingClosureOutcome;
import com.fsc.supply_chain_common_operations.enums.SourcingMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Column;

import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Table(name = "sourcing_requests")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SourcingRequest extends Request {

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private SourcingMethod sourcingMethod;

    @OneToOne
    @JoinColumn(name = "source_purchase_request_sys_id", unique = true)
    private PurchaseRequest sourcePurchaseRequest;

    private Instant publishedAt;

    private Instant closedAt;

    @Column(length = 1000)
    private String closureReason;


    private Long version;

    @ManyToOne
    private Employee submitterEmployee;

    @ManyToOne
    private Employee procurementApprovalManager;

    private Instant responseDeadline;

    private Instant evaluationDeadline;

    private Integer targetSupplierCount;

    @Column(length = 2000)
    private String evaluationCriteria;

    @Column(length = 2000)
    private String commercialTerms;

    @Column(length = 3)
    private String currencyCode;

    // Purchase request commercial snapshots
    @Column(precision = 19, scale = 4)
    private BigDecimal estimatedTotalAmount;

    @Column(length = 500)
    private String deliveryLocation;

    private String budgetCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ProcurementType procurementType;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private SourcingClosureOutcome closureOutcome;
}
