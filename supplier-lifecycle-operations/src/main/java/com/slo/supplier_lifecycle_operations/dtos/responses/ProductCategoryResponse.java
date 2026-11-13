package com.slo.supplier_lifecycle_operations.dtos.responses;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ProductCategoryResponse {

    private UUID sysId;

    private String categoryCode;

    private String categoryName;

    private String description;

    private UUID parentCategorySysId;

    private Integer displayOrder;

    private Boolean active;

    private String imageUrl;
}