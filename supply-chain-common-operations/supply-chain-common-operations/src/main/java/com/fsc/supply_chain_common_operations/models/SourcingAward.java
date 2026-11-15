package com.fsc.supply_chain_common_operations.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "sourcing_awards",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sourcing_award_request",
                columnNames = "sourcing_request_sys_id"
        )
)
public class SourcingAward extends GlobalRecord {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "sourcing_request_sys_id",
            nullable = false,
            unique = true
    )
    private SourcingRequest sourcingRequest;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_bid_sys_id", nullable = false)
    private Bid acceptedBid;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "awarded_by_employee_sys_id", nullable = false)
    private Employee awardedBy;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Column(nullable = false)
    private Instant awardedAt;
}
