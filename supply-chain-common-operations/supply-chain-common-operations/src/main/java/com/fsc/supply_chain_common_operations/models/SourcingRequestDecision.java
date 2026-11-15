package com.fsc.supply_chain_common_operations.models;

import com.fsc.supply_chain_common_operations.enums.RequestStatus;
import com.fsc.supply_chain_common_operations.enums.SourcingRequestDecisionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sourcing_request_decisions")
public class SourcingRequestDecision extends GlobalRecord {

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "sourcing_request_sys_id",
            nullable = false
    )
    private SourcingRequest sourcingRequest;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "actor_employee_sys_id",
            nullable = false
    )
    private Employee actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SourcingRequestDecisionType decisionType;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private RequestStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestStatus toStatus;

    @Column(length = 1000)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime decisionAt;
}
