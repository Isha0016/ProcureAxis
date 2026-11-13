package com.slo.supplier_lifecycle_operations.dtos.responses;

import com.fsc.supply_chain_common_operations.enums.ProductStatus;
import com.fsc.supply_chain_common_operations.enums.ProductType;
import com.fsc.supply_chain_common_operations.enums.UnitOfMeasure;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ProductResponse {

    private UUID sysId;

    private String productCode;

    private String productName;

    private String description;

    private UUID categorySysId;

    private String categoryName;

    private ProductType productType;

    private ProductStatus status;

    private UnitOfMeasure unitOfMeasure;

    private String brand;

    private String manufacturer;

    private String modelNumber;

    private String imageUrl;

    private UUID supplierCompanySysId;

    private String supplierCompanyName;
}
