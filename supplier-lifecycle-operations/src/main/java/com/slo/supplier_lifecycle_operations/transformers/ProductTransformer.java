package com.slo.supplier_lifecycle_operations.transformers;

import com.fsc.supply_chain_common_operations.models.Product;
import com.slo.supplier_lifecycle_operations.dtos.responses.ProductResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductTransformer {

    public ProductResponse toResponse(Product product) {

        return ProductResponse.builder()
                .sysId(product.getSysId())
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .description(product.getDescription())

                .categorySysId(
                        product.getCategory() != null
                                ? product.getCategory().getSysId()
                                : null)

                .categoryName(
                        product.getCategory() != null
                                ? product.getCategory().getCategoryName()
                                : null)

                .productType(product.getProductType())
                .status(product.getStatus())
                .unitOfMeasure(product.getUnitOfMeasure())
                .brand(product.getBrand())
                .manufacturer(product.getManufacturer())
                .modelNumber(product.getModelNumber())
                .imageUrl(product.getImageUrl())

                .supplierCompanySysId(
                        product.getSupplierCompany() != null
                                ? product.getSupplierCompany().getSysId()
                                : null)

                .supplierCompanyName(
                        product.getSupplierCompany() != null
                                ? product.getSupplierCompany().getCompanyName()
                                : null)

                .build();
    }
}