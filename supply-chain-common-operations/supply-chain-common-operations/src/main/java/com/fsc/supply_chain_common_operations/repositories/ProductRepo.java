package com.fsc.supply_chain_common_operations.repositories;

import com.fsc.supply_chain_common_operations.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepo extends JpaRepository<Product, UUID> {

    boolean existsBySupplierCompany_SysIdAndProductCodeIgnoreCase(
            UUID supplierCompanyId,
            String productCode
    );

    List<Product> findBySupplierCompany_SysId(UUID supplierCompanyId);
}