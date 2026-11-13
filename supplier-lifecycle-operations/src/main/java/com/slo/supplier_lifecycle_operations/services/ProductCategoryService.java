package com.slo.supplier_lifecycle_operations.services;

import com.fsc.supply_chain_common_operations.exceptions.DuplicateRecordException;
import com.fsc.supply_chain_common_operations.exceptions.RecordDoesNotExistException;
import com.fsc.supply_chain_common_operations.models.ProductCategory;
import com.fsc.supply_chain_common_operations.repositories.ProductCategoryRepo;
import com.slo.supplier_lifecycle_operations.transformers.ProductCategoryTransformer;
import com.slo.supplier_lifecycle_operations.dtos.responses.ProductCategoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProductCategoryService {

    private final ProductCategoryRepo productCategoryRepo;
    private final ProductCategoryTransformer productCategoryTransformer;

    public ProductCategoryService(ProductCategoryRepo productCategoryRepo ,
                                  ProductCategoryTransformer productCategoryTransformer) {
        this.productCategoryRepo = productCategoryRepo;
        this.productCategoryTransformer=productCategoryTransformer;
    }

    @Transactional
    public ProductCategory createCategory(ProductCategory category) {

        if (category.getCategoryCode() == null ||
                category.getCategoryCode().isBlank()) {
            throw new IllegalArgumentException("Category code is required.");
        }

        if (category.getCategoryName() == null ||
                category.getCategoryName().isBlank()) {
            throw new IllegalArgumentException("Category name is required.");
        }

        category.setCategoryCode(category.getCategoryCode().trim());
        category.setCategoryName(category.getCategoryName().trim());

        if (productCategoryRepo.existsByCategoryCodeIgnoreCase(
                category.getCategoryCode())) {
            throw new DuplicateRecordException(
                    "Category code already exists.");
        }

        if (category.getParentCategory() != null) {
            UUID parentId = category.getParentCategory().getSysId();

            ProductCategory parent = productCategoryRepo.findById(parentId)
                    .orElseThrow(() -> new RecordDoesNotExistException(
                            "Parent category not found: " + parentId));

            if (!Boolean.TRUE.equals(parent.getActive())) {
                throw new IllegalArgumentException(
                        "Parent category must be active.");
            }

            category.setParentCategory(parent);
        }

        category.setActive(true);

        return productCategoryRepo.save(category);
    }

    @Transactional(readOnly = true)
    public List<ProductCategoryResponse> getActiveCategories() {
        return productCategoryRepo
                .findByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(productCategoryTransformer::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductCategory getCategoryById(UUID categoryId) {
        return productCategoryRepo.findById(categoryId)
                .orElseThrow(() -> new RecordDoesNotExistException(
                        "Product category not found: " + categoryId));
    }
}
