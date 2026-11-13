package com.fsc.supply_chain_common_operations.repositories;

import com.fsc.supply_chain_common_operations.models.ProcurementCompany;
import org.hibernate.annotations.Audited;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcurementCompanyRepo
        extends JpaRepository<ProcurementCompany, UUID> {
}
