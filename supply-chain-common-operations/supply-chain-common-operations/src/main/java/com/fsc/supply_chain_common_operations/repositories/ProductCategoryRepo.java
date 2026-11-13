package com.fsc.supply_chain_common_operations.repositories;

import com.fsc.supply_chain_common_operations.models.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductCategoryRepo
        extends JpaRepository<ProductCategory, UUID> {

    boolean existsByCategoryCodeIgnoreCase(String categoryCode);

    List<ProductCategory> findByActiveTrueOrderByDisplayOrderAsc();
}