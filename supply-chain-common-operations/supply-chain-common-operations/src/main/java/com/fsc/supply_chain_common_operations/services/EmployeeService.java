package com.fsc.supply_chain_common_operations.services;

import com.fsc.supply_chain_common_operations.constants.NotificationTemplateConstant;
import com.fsc.supply_chain_common_operations.dtos.requests.CommonValidationDto;
import com.fsc.supply_chain_common_operations.dtos.requests.InviteEmployeeRequest;
import com.fsc.supply_chain_common_operations.enums.CompanyType;
import com.fsc.supply_chain_common_operations.enums.NotificationPriority;
import com.fsc.supply_chain_common_operations.models.*;
import com.fsc.supply_chain_common_operations.repositories.EmployeeRepo;
import com.fsc.supply_chain_common_operations.transformers.EntityTransformer;
import com.fsc.supply_chain_common_operations.utilities.CommonUtility;
import com.fsc.supply_chain_common_operations.utilities.SystemUtility;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fsc.supply_chain_common_operations.repositories.NotificationContextRepo;
import com.fsc.supply_chain_common_operations.repositories.CompanyRepo;
import com.fsc.supply_chain_common_operations.dtos.requests.AssignEmployeeManagerRequest;
import com.fsc.supply_chain_common_operations.enums.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Slf4j
@Service
public class EmployeeService implements EntityService<Employee> {

    private final RoleService roleService;
    private final EmployeeRepo employeeRepo;
    private final CompanyRepo companyRepo;
    private final CommonUtility commonUtility;
    private final NotificationService notificationService;
    @Autowired
    private NotificationContextRepo notificationContextRepo;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public EmployeeService(RoleService roleService,
                           EmployeeRepo employeeRepo,
                           CompanyRepo companyRepo,
                           CommonUtility commonUtility,
                           NotificationService notificationService,
                           PasswordEncoder passwordEncoder){
        this.roleService = roleService;
        this.employeeRepo = employeeRepo;
        this.companyRepo = companyRepo;
        this.commonUtility = commonUtility;
        this.notificationService = notificationService;
        this.passwordEncoder=passwordEncoder;
    }

    public Employee getEmployeeByEmail(String email){
        return this.employeeRepo.findByWorkEmail(email);
    }

    public Employee saveOrUpdate(Employee employee){
        return this.employeeRepo.save(employee);
    }

    public Employee createMaintUserForCompany(Company company, CompanyType companyType) {
        company.setCompanyType(companyType);
        company = companyRepo.save(company);

        log.info("Starting main user creation. companyId={}, companyType={}",
                company.getSysId(), companyType);

        // Create main/admin role for the company
        log.info("Creating main role. companyId={}, companyType={}",
                company.getSysId(), companyType);

        Role maintRole = roleService.createMaintRole(company, companyType);

        log.info("Main role created successfully. companyId={}, roleId={}",
                company.getSysId(), maintRole.getSysId());

        // Generate password and map company details to first employee
        String generatedPassword = SystemUtility.generatePassword(10);

        Employee maintEmployee =
                EntityTransformer.mapCompanyToEmployee(company, generatedPassword);

        maintEmployee.setPasswordHash(passwordEncoder.encode(generatedPassword));

        log.debug("Company mapped to main employee successfully. companyId={}",
                company.getSysId());

        // Assign main/admin role
        maintEmployee.setRoles(List.of(maintRole));

        log.debug("Main role assigned to employee. companyId={}, roleId={}",
                company.getSysId(), maintRole.getSysId());

        // Save employee
        maintEmployee = this.saveOrUpdate(maintEmployee);

        log.info("Main employee created successfully. companyId={}, employeeId={}",
                company.getSysId(), maintEmployee.getSysId());

        // TODO: Send account creation notification to employee.
        log.info("Main user creation completed successfully. companyId={}, employeeId={}",
                company.getSysId(), maintEmployee.getSysId());

        return maintEmployee;
    }

    public Employee getEmployeeBySysId(UUID sysId){
        return employeeRepo.findById(sysId).orElse(null);
    }

    public Employee inviteEmployee(
            InviteEmployeeRequest inviteEmployeeRequest,
            Employee invitorEmp
    ){
      commonUtility.validateBasicChecks(new CommonValidationDto(inviteEmployeeRequest.getRoleSysIds(), invitorEmp.getSysId(),inviteEmployeeRequest.getCompanySysId()));
      // Map the request to the model.
        // Fetch the target company using companySysId
        Company company = companyRepo.findById(
                inviteEmployeeRequest.getCompanySysId()
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "Company not found: "
                                + inviteEmployeeRequest.getCompanySysId()
                )
        );

// Fetch manager only when managerSysId is provided
        Employee manager = null;

        if (inviteEmployeeRequest.getManagerSysId() != null) {
            manager = this.getEmployeeBySysId(
                    inviteEmployeeRequest.getManagerSysId()
            );

            if (manager == null) {
                throw new IllegalArgumentException(
                        "Manager employee not found"
                );
            }

            // Manager must belong to the target company
            if (!manager.getCompany().getSysId()
                    .equals(company.getSysId())) {
                throw new IllegalArgumentException(
                        "Manager does not belong to the target company"
                );
            }
        }

// Fetch roles assigned to the employee
        List<Role> roles = roleService.fetchRolesBySysId(
                inviteEmployeeRequest.getRoleSysIds()
        );

        String temporaryPassword = SystemUtility.generatePassword(10);

// Map employee using the target company, not the invitor's company
        Employee employee = EntityTransformer.mapEmpRequestToEmpModel(
                inviteEmployeeRequest,
                company,
                invitorEmp,
                manager,
                roles,
                temporaryPassword
        );

        employee.setPasswordHash(passwordEncoder.encode(temporaryPassword));

        employee = this.saveOrUpdate(employee);
      employee = this.saveOrUpdate(employee);
        HashMap<String, String> emailMap = new HashMap<>();
        emailMap.put("temporaryPassword", temporaryPassword);

// User Details
        emailMap.put("maintUserName", employee.getFirstName());
        emailMap.put("maintUserEmail", employee.getEmailAddress());

// Company Details
        emailMap.put("companyName", employee.getCompany().getCompanyName());

// Employee Details
        emailMap.put("employeeCode", employee.getEmployeeCode());
        emailMap.put("designation", employee.getDesignation());
        emailMap.put("department", employee.getDepartment());

        emailMap.put("managerName",
                employee.getManager() != null
                        ? employee.getManager().getFirstName()
                        : "Not Assigned");

        emailMap.put("employmentType",
                employee.getEmploymentType() != null
                        ? employee.getEmploymentType().name()
                        : "Not Specified");

        emailMap.put("joiningDate",
                employee.getJoiningDate() != null
                        ? employee.getJoiningDate().toString()
                        : "Not Specified");

        emailMap.put("workEmail", employee.getWorkEmail());
        emailMap.put("workPhone", employee.getWorkPhone());
        emailMap.put("officeLocation", employee.getOfficeLocation());

// Application Details
        emailMap.put("applicationUrl", "https://google.com");
        emailMap.put("currentYear", String.valueOf(LocalDate.now().getYear()));

        HashMap<String, String> subjectMap = new HashMap<>();

        subjectMap.put("companyName",
                employee.getCompany().getCompanyName());

        NotificationContext notificationContext = new NotificationContext(
                SystemUtility.generateId("NOTIFICATION-CONTEXT"),
                subjectMap,
                emailMap,
                new HashMap<>(),
                new HashMap<>()
        );
        notificationContext = notificationContextRepo.save(notificationContext);

        NotificationTemplate notificationTemplate = notificationService.fetchTemplateById(NotificationTemplateConstant.COMPANY_REGISTRATION_TEMPLATE_ID);
        Notification inviteNotification = new Notification();
        inviteNotification.setNotificationId(SystemUtility.generateId("NOTIFICATION"));
        inviteNotification.setBccUsers(List.of(invitorEmp));
        inviteNotification.setNotificationContext(notificationContext);
        inviteNotification.setEmail(true);
        inviteNotification.setNotificationPriority(NotificationPriority.URGENT);
        inviteNotification.setToUsers(List.of(employee));
        inviteNotification.setCreatedAt(LocalDateTime.now());
        inviteNotification.setUpdatedAt(LocalDateTime.now());
        inviteNotification.setCreatedBy(invitorEmp.getEmailAddress());
        inviteNotification.setUpdatedBy(invitorEmp.getEmailAddress());
        inviteNotification.setNotificationTemplate(notificationTemplate);

        notificationService.sendNotification(inviteNotification);
      return employee;
    }

    public Employee assignManager(
            UUID employeeSysId,
            AssignEmployeeManagerRequest request
    ) {
        Employee employee = employeeRepo.findById(employeeSysId)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found"));

        Employee manager = employeeRepo.findById(request.getManagerSysId())
                .orElseThrow(() ->
                        new RuntimeException("Manager not found"));

        if (employee.getSysId().equals(manager.getSysId())) {
            throw new RuntimeException("Employee cannot be their own manager");
        }

        if (!employee.getCompany().getSysId()
                .equals(manager.getCompany().getSysId())) {
            throw new RuntimeException(
                    "Employee and manager must belong to the same company");
        }

        if (manager.getUserStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException("Manager account is not active");
        }

        boolean canApprove = manager.getRoles().stream()
                .filter(role -> role != null && role.getOperations() != null)
                .flatMap(role -> role.getOperations().stream())
                .anyMatch(operation ->
                        "APPROVE_PURCHASE_REQUEST".equals(
                                operation.getOperationName()));

        if (!canApprove) {
            throw new RuntimeException(
                    "Manager does not have APPROVE_PURCHASE_REQUEST permission");
        }

        employee.setManager(manager);

        return employeeRepo.save(employee);
    }
}
