package com.slo.supplier_lifecycle_operations.services;

import com.fsc.supply_chain_common_operations.enums.CompanyStatus;
import com.fsc.supply_chain_common_operations.enums.ProductStatus;
import com.fsc.supply_chain_common_operations.exceptions.DuplicateRecordException;
import com.fsc.supply_chain_common_operations.exceptions.RecordDoesNotExistException;
import com.fsc.supply_chain_common_operations.models.Product;
import com.fsc.supply_chain_common_operations.models.ProductCategory;
import com.fsc.supply_chain_common_operations.models.SupplierCompany;
import com.fsc.supply_chain_common_operations.repositories.ProductCategoryRepo;
import com.fsc.supply_chain_common_operations.repositories.ProductRepo;
import com.fsc.supply_chain_common_operations.repositories.SupplierCompanyRepo;
import com.fsc.supply_chain_common_operations.enums.ProductType;
import com.fsc.supply_chain_common_operations.enums.UnitOfMeasure;
import io.jsonwebtoken.Claims;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepo productRepo;
    private final ProductCategoryRepo productCategoryRepo;
    private final SupplierCompanyRepo supplierCompanyRepo;

    public ProductService(
            ProductRepo productRepo,
            ProductCategoryRepo productCategoryRepo,
            SupplierCompanyRepo supplierCompanyRepo) {

        this.productRepo = productRepo;
        this.productCategoryRepo = productCategoryRepo;
        this.supplierCompanyRepo = supplierCompanyRepo;
    }

    private UUID getAuthenticatedCompanyId() {

        Claims claims = (Claims) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String companyId = claims.get("companyId", String.class);

        if (companyId == null || companyId.isBlank()) {
            throw new AccessDeniedException(
                    "Authenticated user has no company assigned.");
        }

        return UUID.fromString(companyId);
    }

    private SupplierCompany getActiveSupplier() {

        UUID companyId = getAuthenticatedCompanyId();

        SupplierCompany supplier = supplierCompanyRepo.findById(companyId)
                .orElseThrow(() -> new RecordDoesNotExistException(
                        "Supplier company not found: " + companyId));

        if (supplier.getStatus() != CompanyStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Only active suppliers can manage products.");
        }

        return supplier;
    }

    @Transactional
    public Product createProduct(Product product) {

        SupplierCompany supplier = getActiveSupplier();

        if (product.getProductCode() == null ||
                product.getProductCode().isBlank()) {
            throw new IllegalArgumentException(
                    "Product code is required.");
        }

        if (product.getProductName() == null ||
                product.getProductName().isBlank()) {
            throw new IllegalArgumentException(
                    "Product name is required.");
        }

        product.setProductCode(product.getProductCode().trim());
        product.setProductName(product.getProductName().trim());

        if (productRepo
                .existsBySupplierCompany_SysIdAndProductCodeIgnoreCase(
                        supplier.getSysId(),
                        product.getProductCode())) {
            throw new DuplicateRecordException(
                    "Product code already exists for this supplier.");
        }

        if (product.getCategory() == null ||
                product.getCategory().getSysId() == null) {
            throw new IllegalArgumentException(
                    "Product category is required.");
        }

        UUID categoryId = product.getCategory().getSysId();

        ProductCategory category = productCategoryRepo.findById(categoryId)
                .orElseThrow(() -> new RecordDoesNotExistException(
                        "Product category not found: " + categoryId));

        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new IllegalArgumentException(
                    "Cannot assign an inactive category.");
        }

        if (product.getUnitOfMeasure() == null) {
            throw new IllegalArgumentException(
                    "Unit of measure is required.");
        }

        // Ownership must come from the authenticated user,
        // never from the request body.
        product.setSupplierCompany(supplier);

        // New products always start as DRAFT.
        product.setStatus(ProductStatus.DRAFT);

        product.setCategory(category);
        System.out.println("Product service reached");
        return productRepo.save(product);
    }

    @Transactional(readOnly = true)
    public List<Product> getMyProducts() {

        SupplierCompany supplier = getActiveSupplier();

        return productRepo.findBySupplierCompany_SysId(
                supplier.getSysId());
    }

    @Transactional(readOnly = true)
    public List<Product> getMyDraftProducts() {

        // Identify the authenticated supplier
        SupplierCompany supplier = getActiveSupplier();

        // Retrieve only this supplier's DRAFT products
        return productRepo.findBySupplierCompany_SysIdAndStatus(
                supplier.getSysId(),
                ProductStatus.DRAFT
        );
    }

    @Transactional(readOnly = true)
    public Page<Product> browseActiveProducts(
            String productName,
            UUID categoryId,
            ProductType productType,
            UnitOfMeasure unitOfMeasure,
            Pageable pageable) {

        return productRepo.searchActiveProducts(
                ProductStatus.ACTIVE,
                CompanyStatus.ACTIVE,
                productName==null ?"": productName.trim(),
                categoryId,
                productType,
                unitOfMeasure,
                pageable
        );
    }

    @Transactional
    public Product updateProduct(UUID productId, Product updatedProduct) {

        SupplierCompany supplier = getActiveSupplier();

        Product existingProduct = productRepo.findById(productId)
                .orElseThrow(() -> new RecordDoesNotExistException(
                        "Product not found: " + productId));

        if (!existingProduct.getSupplierCompany().getSysId()
                .equals(supplier.getSysId())) {
            throw new AccessDeniedException(
                    "You are not authorized to update this product.");
        }

        if (updatedProduct.getProductName() != null) {
            if (updatedProduct.getProductName().isBlank()) {
                throw new IllegalArgumentException(
                        "Product name cannot be blank.");
            }

            existingProduct.setProductName(
                    updatedProduct.getProductName().trim());
        }

        if (updatedProduct.getDescription() != null) {
            existingProduct.setDescription(
                    updatedProduct.getDescription());
        }

        if (updatedProduct.getCategory() != null) {

            UUID categoryId =
                    updatedProduct.getCategory().getSysId();

            ProductCategory category =
                    productCategoryRepo.findById(categoryId)
                            .orElseThrow(() ->
                                    new RecordDoesNotExistException(
                                            "Product category not found: "
                                                    + categoryId));

            if (!Boolean.TRUE.equals(category.getActive())) {
                throw new IllegalArgumentException(
                        "Cannot assign an inactive category.");
            }

            existingProduct.setCategory(category);
        }

        if (updatedProduct.getUnitOfMeasure() != null) {
            existingProduct.setUnitOfMeasure(
                    updatedProduct.getUnitOfMeasure());
        }

        if (updatedProduct.getProductType() != null) {
            existingProduct.setProductType(
                    updatedProduct.getProductType());
        }

        if (updatedProduct.getBrand() != null) {
            existingProduct.setBrand(updatedProduct.getBrand());
        }

        if (updatedProduct.getManufacturer() != null) {
            existingProduct.setManufacturer(
                    updatedProduct.getManufacturer());
        }

        if (updatedProduct.getModelNumber() != null) {
            existingProduct.setModelNumber(
                    updatedProduct.getModelNumber());
        }

        if (updatedProduct.getImageUrl() != null) {
            existingProduct.setImageUrl(updatedProduct.getImageUrl());
        }

        return productRepo.save(existingProduct);
    }

    @Transactional
    public Product updateProductStatus(UUID productId, ProductStatus newStatus) {

        SupplierCompany supplier = getActiveSupplier();

        Product existingProduct = productRepo.findById(productId)
                .orElseThrow(() -> new RecordDoesNotExistException(
                        "Product not found: " + productId));

        // Verify product ownership
        if (!existingProduct.getSupplierCompany().getSysId()
                .equals(supplier.getSysId())) {
            throw new AccessDeniedException(
                    "You are not authorized to update this product.");
        }

        ProductStatus currentStatus = existingProduct.getStatus();

        if (newStatus == null) {
            throw new IllegalArgumentException(
                    "Product status is required.");
        }

        // Validate allowed status transitions
        boolean allowed = switch (currentStatus) {
            case DRAFT ->
                    newStatus == ProductStatus.ACTIVE ||
                            newStatus == ProductStatus.INACTIVE;

            case ACTIVE ->
                    newStatus == ProductStatus.INACTIVE ||
                            newStatus == ProductStatus.DISCONTINUED;

            case INACTIVE ->
                    newStatus == ProductStatus.ACTIVE ||
                            newStatus == ProductStatus.DISCONTINUED;

            case DISCONTINUED -> false;
        };

        if (!allowed) {
            throw new IllegalArgumentException(
                    "Invalid product status transition: "
                            + currentStatus + " -> " + newStatus);
        }

        existingProduct.setStatus(newStatus);

        return productRepo.save(existingProduct);
    }
}