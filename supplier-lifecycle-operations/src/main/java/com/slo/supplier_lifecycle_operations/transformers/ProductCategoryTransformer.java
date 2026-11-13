package com.slo.supplier_lifecycle_operations.transformers;

import com.fsc.supply_chain_common_operations.models.ProductCategory;
import com.slo.supplier_lifecycle_operations.dtos.responses.ProductCategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductCategoryTransformer {

    public ProductCategoryResponse toResponse(ProductCategory category) {

        return ProductCategoryResponse.builder()
                .sysId(category.getSysId())
                .categoryCode(category.getCategoryCode())
                .categoryName(category.getCategoryName())
                .description(category.getDescription())
                .parentCategorySysId(
                        category.getParentCategory() != null
                                ? category.getParentCategory().getSysId()
                                : null)
                .displayOrder(category.getDisplayOrder())
                .active(category.getActive())
                .imageUrl(category.getImageUrl())
                .build();
    }
}
