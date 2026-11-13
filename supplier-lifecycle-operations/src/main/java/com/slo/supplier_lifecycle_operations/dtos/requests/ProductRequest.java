package com.slo.supplier_lifecycle_operations.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

import com.fsc.supply_chain_common_operations.enums.ProductType;
import com.fsc.supply_chain_common_operations.enums.UnitOfMeasure;

@Data
public class ProductRequest {

    @NotBlank(message = "Product code is required")
    @Size(max = 100)
    private String productCode;

    @NotBlank(message = "Product name is required")
    @Size(max = 255)
    private String productName;

    private String description;

    @NotNull(message = "Product category is required")
    private UUID categorySysId;

    @NotNull(message = "Product type is required")
    private ProductType productType;

    @NotNull(message = "Unit of measure is required")
    private UnitOfMeasure unitOfMeasure;

    private String brand;

    private String manufacturer;

    private String modelNumber;

    private String imageUrl;
}