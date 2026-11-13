package com.fsc.supply_chain_common_operations.repositories;

import com.fsc.supply_chain_common_operations.enums.CompanyStatus;
import com.fsc.supply_chain_common_operations.enums.ProductStatus;
import com.fsc.supply_chain_common_operations.models.Product;
import com.fsc.supply_chain_common_operations.enums.ProductType;
import com.fsc.supply_chain_common_operations.enums.UnitOfMeasure;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<Product> findByStatusAndSupplierCompany_Status(
            ProductStatus productStatus,
            CompanyStatus supplierStatus,
            Pageable pageable
    );

    @Query("""
    SELECT p FROM Product p
    WHERE p.status = :productStatus
      AND p.supplierCompany.status = :supplierStatus
      AND (:productName IS NULL
           OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :productName, '%')))
      AND (:categoryId IS NULL
           OR p.category.sysId = :categoryId)
      AND (:productType IS NULL
           OR p.productType = :productType)
      AND (:unitOfMeasure IS NULL
           OR p.unitOfMeasure = :unitOfMeasure)
    """)
    Page<Product> searchActiveProducts(
            @Param("productStatus") ProductStatus productStatus,
            @Param("supplierStatus") CompanyStatus supplierStatus,
            @Param("productName") String productName,
            @Param("categoryId") UUID categoryId,
            @Param("productType") ProductType productType,
            @Param("unitOfMeasure") UnitOfMeasure unitOfMeasure,
            Pageable pageable
    );

    List<Product> findBySupplierCompany_SysIdAndStatus(
            UUID supplierCompanyId,
            ProductStatus status
    );
}