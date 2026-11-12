package com.spo.sourcing_and_procurement_operations.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcurementCompanyRegistrationRequest {

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

    // Procurement-specific details

    @Min(value = 1, message = "Fiscal year start month must be between 1 and 12")
    @Max(value = 12, message = "Fiscal year start month must be between 1 and 12")
    private Integer fiscalYearStartMonth;

    private Boolean approvalRequired;
}
