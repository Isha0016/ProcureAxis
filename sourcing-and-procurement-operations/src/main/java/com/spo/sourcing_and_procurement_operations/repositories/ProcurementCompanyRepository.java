package com.spo.sourcing_and_procurement_operations.repositories;

import com.fsc.supply_chain_common_operations.models.ProcurementCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcurementCompanyRepository
        extends JpaRepository<ProcurementCompany, UUID> {
}
