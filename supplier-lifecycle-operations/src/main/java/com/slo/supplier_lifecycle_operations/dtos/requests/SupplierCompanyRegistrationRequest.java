package com.slo.supplier_lifecycle_operations.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierCompanyRegistrationRequest {

    // Supplier details

    @NotBlank(message = "Supplier category is required")
    private String supplierCategory;

    @Positive(message = "Lead time must be greater than zero")
    private Integer leadTimeDays;

    @Positive(message = "Minimum order value must be greater than zero")
    private BigDecimal minimumOrderValue;

    // Company details

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Legal name is required")
    private String legalName;

    private String gstNumber;

    private String panNumber;

    @NotBlank(message = "Email address is required")
    @Email(message = "Please provide a valid email address")
    private String emailAddress;

    private String website;

    private String contactNumber;
}
