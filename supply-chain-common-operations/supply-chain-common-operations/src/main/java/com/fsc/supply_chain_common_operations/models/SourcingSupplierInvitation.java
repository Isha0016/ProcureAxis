package com.fsc.supply_chain_common_operations.models;

import com.fsc.supply_chain_common_operations.enums.SourcingInvitationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(
        name = "sourcing_supplier_invitations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sourcing_request_supplier",
                        columnNames = {
                                "sourcing_request_sys_id",
                                "supplier_company_sys_id"
                        }
                )
        }
)
public class SourcingSupplierInvitation extends GlobalRecord {

    @ManyToOne(optional = false)
    @JoinColumn(name = "sourcing_request_sys_id", nullable = false)
    private SourcingRequest sourcingRequest;

    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_company_sys_id", nullable = false)
    private SupplierCompany supplierCompany;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SourcingInvitationStatus status;

    private Instant invitedAt;
}