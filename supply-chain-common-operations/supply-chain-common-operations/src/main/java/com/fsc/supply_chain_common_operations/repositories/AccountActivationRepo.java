package com.fsc.supply_chain_common_operations.repositories;

import com.fsc.supply_chain_common_operations.models.AccountActivation;
import com.fsc.supply_chain_common_operations.models.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountActivationRepo
        extends JpaRepository<AccountActivation, UUID> {

    Optional<AccountActivation> findByTokenHash(String tokenHash);

    List<AccountActivation> findByEmployeeAndConsumedAtIsNull(
            Employee employee
    );
}