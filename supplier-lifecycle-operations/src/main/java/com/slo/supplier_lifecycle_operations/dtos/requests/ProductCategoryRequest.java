package com.slo.supplier_lifecycle_operations.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class ProductCategoryRequest {

    @NotBlank(message = "Category code is required")
    @Size(max = 50)
    private String categoryCode;

    @NotBlank(message = "Category name is required")
    @Size(max = 150)
    private String categoryName;

    @Size(max = 1000)
    private String description;

    private UUID parentCategorySysId;

    private Integer displayOrder;

    private String imageUrl;
}
