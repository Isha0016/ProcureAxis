package com.fsc.supply_chain_common_operations.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(
        name = "account_activations",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "token_hash")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountActivation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID sysId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_sys_id", nullable = false)
    private Employee employee;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}