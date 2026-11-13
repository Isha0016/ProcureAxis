package com.slo.supplier_lifecycle_operations.controllers;

import com.fsc.supply_chain_common_operations.services.AuthService;
import com.slo.supplier_lifecycle_operations.dtos.responses.ProductCategoryResponse;
import com.slo.supplier_lifecycle_operations.services.ProductCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fsc.supply_chain_common_operations.models.ProductCategory;
import com.slo.supplier_lifecycle_operations.dtos.requests.ProductCategoryRequest;
import com.slo.supplier_lifecycle_operations.transformers.ProductCategoryTransformer;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
@RequestMapping("/api/v1/slo/product-categories")
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;
    private final AuthService authService;
    private final ProductCategoryTransformer productCategoryTransformer;

    @Autowired
    public ProductCategoryController(
            ProductCategoryService productCategoryService,
            AuthService authService,
            ProductCategoryTransformer productCategoryTransformer) {

        this.productCategoryService = productCategoryService;
        this.authService = authService;
        this.productCategoryTransformer = productCategoryTransformer;
    }

    @GetMapping
    public ResponseEntity<List<ProductCategoryResponse>> getActiveCategories(
            @RequestHeader("token") String token) {

        List<ProductCategoryResponse> categories =
                productCategoryService.getActiveCategories();

        return ResponseEntity.ok(categories);
    }
    @PostMapping
    public ResponseEntity<ProductCategoryResponse> createCategory(
            @RequestHeader("token") String token,
            @Valid @RequestBody ProductCategoryRequest request) {

        // Only an authorized internal employee can create categories.
        authService.isUserHavingAccess(token, "CREATE_PRODUCT");

        ProductCategory category = new ProductCategory();

        category.setCategoryCode(request.getCategoryCode());
        category.setCategoryName(request.getCategoryName());
        category.setDescription(request.getDescription());
        category.setDisplayOrder(request.getDisplayOrder());
        category.setImageUrl(request.getImageUrl());

        if (request.getParentCategorySysId() != null) {
            ProductCategory parent = new ProductCategory();
            parent.setSysId(request.getParentCategorySysId());
            category.setParentCategory(parent);
        }

        ProductCategory savedCategory =
                productCategoryService.createCategory(category);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productCategoryTransformer.toResponse(savedCategory));
    }
}
