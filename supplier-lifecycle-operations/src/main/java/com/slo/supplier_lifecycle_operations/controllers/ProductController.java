package com.slo.supplier_lifecycle_operations.controllers;

import com.fsc.supply_chain_common_operations.models.Product;
import com.fsc.supply_chain_common_operations.models.ProductCategory;
import com.slo.supplier_lifecycle_operations.dtos.requests.ProductRequest;
import com.slo.supplier_lifecycle_operations.dtos.responses.ProductResponse;
import com.slo.supplier_lifecycle_operations.services.ProductService;
import com.slo.supplier_lifecycle_operations.transformers.ProductTransformer;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/slo/products")
public class ProductController {

    private final ProductService productService;
    private final ProductTransformer productTransformer;

    public ProductController(
            ProductService productService,
            ProductTransformer productTransformer) {

        this.productService = productService;
        this.productTransformer = productTransformer;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {

        Product product = new Product();

        product.setProductCode(request.getProductCode());
        product.setProductName(request.getProductName());
        product.setDescription(request.getDescription());
        product.setProductType(request.getProductType());
        product.setUnitOfMeasure(request.getUnitOfMeasure());
        product.setBrand(request.getBrand());
        product.setManufacturer(request.getManufacturer());
        product.setModelNumber(request.getModelNumber());
        product.setImageUrl(request.getImageUrl());

        ProductCategory category = new ProductCategory();
        category.setSysId(request.getCategorySysId());
        product.setCategory(category);

        Product savedProduct = productService.createProduct(product);
        System.out.println("Product controller reached");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productTransformer.toResponse(savedProduct));
    }

    @GetMapping("/my-products")
    public ResponseEntity<List<ProductResponse>> getMyProducts() {

        List<ProductResponse> products = productService.getMyProducts()
                .stream()
                .map(productTransformer::toResponse)
                .toList();

        return ResponseEntity.ok(products);
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID productId,
            @RequestBody ProductRequest request) {

        Product updatedProduct = new Product();

        updatedProduct.setProductName(request.getProductName());
        updatedProduct.setDescription(request.getDescription());
        updatedProduct.setProductType(request.getProductType());
        updatedProduct.setUnitOfMeasure(request.getUnitOfMeasure());
        updatedProduct.setBrand(request.getBrand());
        updatedProduct.setManufacturer(request.getManufacturer());
        updatedProduct.setModelNumber(request.getModelNumber());
        updatedProduct.setImageUrl(request.getImageUrl());

        if (request.getCategorySysId() != null) {
            ProductCategory category = new ProductCategory();
            category.setSysId(request.getCategorySysId());
            updatedProduct.setCategory(category);
        }

        Product savedProduct =
                productService.updateProduct(productId, updatedProduct);

        return ResponseEntity.ok(
                productTransformer.toResponse(savedProduct));
    }
}