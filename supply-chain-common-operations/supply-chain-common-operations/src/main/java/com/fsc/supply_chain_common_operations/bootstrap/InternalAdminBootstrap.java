package com.fsc.supply_chain_common_operations.bootstrap;

import com.fsc.supply_chain_common_operations.enums.CompanyStatus;
import com.fsc.supply_chain_common_operations.enums.CompanyType;
import com.fsc.supply_chain_common_operations.enums.EmploymentStatus;
import com.fsc.supply_chain_common_operations.enums.UserStatus;
import com.fsc.supply_chain_common_operations.models.Company;
import com.fsc.supply_chain_common_operations.models.Employee;
import com.fsc.supply_chain_common_operations.models.Role;
import com.fsc.supply_chain_common_operations.repositories.CompanyRepo;
import com.fsc.supply_chain_common_operations.services.EmployeeService;
import com.fsc.supply_chain_common_operations.services.RoleService;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Component
public class InternalAdminBootstrap {

    private final CompanyRepo companyRepo;
    private final EmployeeService employeeService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    @Value("${fsc.bootstrap.internal-admin.enabled:false}")
    private boolean enabled;

    @Value("${fsc.bootstrap.internal-admin.email:}")
    private String adminEmail;

    @Value("${fsc.bootstrap.internal-admin.password:}")
    private String adminPassword;

    public InternalAdminBootstrap(
            CompanyRepo companyRepo,
            EmployeeService employeeService,
            RoleService roleService,
            PasswordEncoder passwordEncoder) {

        this.companyRepo = companyRepo;
        this.employeeService = employeeService;
        this.roleService = roleService;
        this.passwordEncoder=passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeInternalAdmin() {

        if (!enabled) {
            return;
        }

        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "Internal admin email and password must be configured."
            );
        }

        Employee existingEmployee =
                employeeService.getEmployeeByEmail(adminEmail);

        if (existingEmployee != null) {
            if (existingEmployee.getUserStatus() != UserStatus.ACTIVE) {
                throw new IllegalStateException(
                        "Internal admin already exists but is not active. "
                                + "Use the approved account activation process."
                );
            }

            String storedPassword = existingEmployee.getPasswordHash();

            if (storedPassword != null
                    && storedPassword.equals(adminPassword)) {
                existingEmployee.setPasswordHash(
                        passwordEncoder.encode(adminPassword)
                );
                employeeService.saveOrUpdate(existingEmployee);
            }

            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. Create internal company
        Company company = new Company();

        company.setCompanyName("FSC Internal");
        company.setLegalName("FSC Internal Administration");
        company.setEmailAddress(adminEmail);
        company.setCompanyCode("FSC-INTERNAL-001");
        company.setCompanyType(CompanyType.INTERNAL);
        company.setStatus(CompanyStatus.ACTIVE);
        company.setCreatedAt(now);
        company.setUpdatedAt(now);
        company.setCreatedBy("system");
        company.setUpdatedBy("system");

        company = companyRepo.save(company);

        // 2. Create internal administrator role
        Role adminRole =
                roleService.createMaintRole(
                        company,
                        CompanyType.INTERNAL
                );

        // 3. Create active internal administrator
        Employee employee = Employee.builder()
                .firstName("FSC")
                .lastName("Administrator")
                .emailAddress(adminEmail)
                .workEmail(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .emailVerified(true)
                .phoneVerified(false)
                .userStatus(UserStatus.ACTIVE)
                .employeeCode("FSC-ADMIN-001")
                .company(company)
                .manager(null)
                .designation("INTERNAL_ADMIN")
                .department("ADMINISTRATION")
                .joiningDate(LocalDate.now())
                .employmentStatus(EmploymentStatus.ACTIVE)
                .roles(List.of(adminRole))
                .createdAt(now)
                .updatedAt(now)
                .createdBy("system")
                .updatedBy("system")
                .build();

        employeeService.saveOrUpdate(employee);
    }
}