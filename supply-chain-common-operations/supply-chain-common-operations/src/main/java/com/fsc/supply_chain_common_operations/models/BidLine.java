package com.fsc.supply_chain_common_operations.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "bid_lines",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_bid_sourcing_line",
                columnNames = {
                        "bid_sys_id",
                        "sourcing_request_line_sys_id"
                }
        )
)
public class BidLine extends GlobalRecord {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_sys_id", nullable = false)
    private Bid bid;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sourcing_request_line_sys_id", nullable = false)
    private SourcingRequestLine sourcingRequestLine;

    @Column(nullable = false, length = 2000)
    private String itemDescriptionSnapshot;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quotedQuantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quotedUnitPrice;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal lineAmount;

    @Column(nullable = false, length = 3)
    private String currencyCode;

    @Column(length = 2000)
    private String remarks;
}