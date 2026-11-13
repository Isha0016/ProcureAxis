package com.fsc.supply_chain_common_operations.securities;

import com.fsc.supply_chain_common_operations.enums.CompanyStatus;
import com.fsc.supply_chain_common_operations.enums.CompanyType;
import com.fsc.supply_chain_common_operations.models.Company;
import com.fsc.supply_chain_common_operations.repositories.CompanyRepo;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("companyAccess")
public class CompanyAccess {

    private final CompanyRepo companyRepo;

    public CompanyAccess(CompanyRepo companyRepo) {
        this.companyRepo = companyRepo;
    }

    public boolean isProcurementCompany(Authentication authentication) {
        return hasActiveCompanyType(
                authentication,
                CompanyType.PROCUREMENT_COMPANY
        );
    }

    public boolean isSupplierCompany(Authentication authentication) {
        return hasActiveCompanyType(
                authentication,
                CompanyType.SUPPLIER_COMPANY
        );
    }

    private boolean hasActiveCompanyType(
            Authentication authentication,
            CompanyType requiredType) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {
            return false;
        }

        // JwtFilter stores the JWT Claims as the principal
        if (!(authentication.getPrincipal() instanceof Claims claims)) {
            return false;
        }

        String companyId = claims.get("companyId", String.class);

        if (companyId == null || companyId.isBlank()) {
            return false;
        }

        try {
            UUID companySysId = UUID.fromString(companyId);

            Company company = companyRepo.findById(companySysId)
                    .orElse(null);

            if (company == null) {
                return false;
            }

            return company.getCompanyType() == requiredType
                    && company.getStatus() == CompanyStatus.ACTIVE;

        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
