package com.fsc.supply_chain_common_operations.services;

import com.fsc.supply_chain_common_operations.constants.NotificationTemplateConstant;
import com.fsc.supply_chain_common_operations.dtos.requests.CommonValidationDto;
import com.fsc.supply_chain_common_operations.dtos.requests.InviteEmployeeRequest;
import com.fsc.supply_chain_common_operations.dtos.requests.ActivateEmployeeRequest;
import com.fsc.supply_chain_common_operations.dtos.requests.AssignEmployeeManagerRequest;
import com.fsc.supply_chain_common_operations.enums.CompanyType;
import com.fsc.supply_chain_common_operations.enums.NotificationPriority;
import com.fsc.supply_chain_common_operations.enums.NotificationStatus;
import com.fsc.supply_chain_common_operations.enums.UserStatus;
import com.fsc.supply_chain_common_operations.models.*;
import com.fsc.supply_chain_common_operations.repositories.EmployeeRepo;
import com.fsc.supply_chain_common_operations.repositories.AccountActivationRepo;
import com.fsc.supply_chain_common_operations.repositories.NotificationContextRepo;
import com.fsc.supply_chain_common_operations.repositories.CompanyRepo;
import com.fsc.supply_chain_common_operations.transformers.EntityTransformer;
import com.fsc.supply_chain_common_operations.utilities.CommonUtility;
import com.fsc.supply_chain_common_operations.utilities.SystemUtility;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
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
    private final PasswordEncoder passwordEncoder;
    private final AccountActivationRepo accountActivationRepo;
    @Value("${fsc.application.base-url:http://localhost:8083}")
    private String applicationBaseUrl;

    @Autowired
    private NotificationContextRepo notificationContextRepo;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    public EmployeeService(
            RoleService roleService,
            EmployeeRepo employeeRepo,
            CompanyRepo companyRepo,
            CommonUtility commonUtility,
            NotificationService notificationService,
            PasswordEncoder passwordEncoder,
            AccountActivationRepo accountActivationRepo
    ) {
        this.roleService = roleService;
        this.employeeRepo = employeeRepo;
        this.companyRepo = companyRepo;
        this.commonUtility = commonUtility;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
        this.accountActivationRepo = accountActivationRepo;
    }

    public Employee getEmployeeByEmail(String email) {
        return employeeRepo.findByWorkEmail(email);
    }

    public Employee saveOrUpdate(Employee employee) {
        return employeeRepo.save(employee);
    }

    // =========================================================
    // MAIN SUPPLIER EMPLOYEE CREATION
    // =========================================================

    @Transactional
    public Employee createMaintUserForCompany(
            Company company,
            CompanyType companyType
    ) {
        return createMaintUserForCompanyWithCredentials(
                company,
                companyType
        ).employee();
    }

    @Transactional
    public MaintUserCreationResult createMaintUserForCompanyWithCredentials(
            Company company,
            CompanyType companyType
    ) {

        // 1. Save company
        company.setCompanyType(companyType);
        company = companyRepo.save(company);

        // 2. Create main employee role
        Role maintRole =
                roleService.createMaintRole(company, companyType);

        // 3. Create main employee without temporary password
        Employee employee =
                EntityTransformer.mapCompanyToEmployee(company, null);

        employee.setRoles(List.of(maintRole));
        employee.setPasswordHash(null);
        employee.setEmailVerified(false);
        employee.setUserStatus(UserStatus.PENDING_VERIFICATION);

        // 4. Save employee
        employee = employeeRepo.save(employee);

        // 5. Generate activation token
        String activationToken = createActivationToken(employee);

        // 6. Send activation notification
        sendActivationNotification(employee, null, activationToken);

        return new MaintUserCreationResult(employee, null);
    }

    // Retained for compatibility with existing callers.
    // No temporary password is generated or returned.
    public record MaintUserCreationResult(
            Employee employee,
            String temporaryPassword
    ) {
    }

    // =========================================================
    // EMPLOYEE INVITATION
    // =========================================================

    @Transactional
    public Employee inviteEmployee(
            InviteEmployeeRequest inviteEmployeeRequest,
            Employee invitorEmp
    ) {

        // 1. Validate inviter, company and requested roles
        commonUtility.validateBasicChecks(
                new CommonValidationDto(
                        inviteEmployeeRequest.getRoleSysIds(),
                        invitorEmp.getSysId(),
                        inviteEmployeeRequest.getCompanySysId()
                )
        );

        // 2. Fetch company
        Company company = companyRepo
                .findById(inviteEmployeeRequest.getCompanySysId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Company not found")
                );

        // 3. Validate manager, if provided
        Employee manager = null;

        if (inviteEmployeeRequest.getManagerSysId() != null) {

            manager = employeeRepo
                    .findById(inviteEmployeeRequest.getManagerSysId())
                    .orElseThrow(() ->
                            new IllegalArgumentException("Manager not found")
                    );

            if (!manager.getCompany().getSysId()
                    .equals(company.getSysId())) {

                throw new IllegalArgumentException(
                        "Manager must belong to the same company"
                );
            }
        }

        // 4. Fetch roles
        List<Role> roles = roleService.fetchRolesBySysId(
                inviteEmployeeRequest.getRoleSysIds()
        );

        // 5. Create employee without password
        Employee employee = EntityTransformer.mapEmpRequestToEmpModel(
                inviteEmployeeRequest,
                company,
                invitorEmp,
                manager,
                roles,
                null
        );

        employee.setManager(manager);
        employee.setCompany(company);
        employee.setRoles(roles);
        employee.setPasswordHash(null);
        employee.setEmailVerified(false);
        employee.setUserStatus(UserStatus.PENDING_VERIFICATION);

        // 6. Save employee once
        employee = employeeRepo.save(employee);

        // 7. Generate activation token
        String activationToken = createActivationToken(employee);

        // 8. Send activation notification
        sendActivationNotification(
                employee,
                invitorEmp,
                activationToken
        );

        return employee;
    }

    // =========================================================
    // COMMON ACTIVATION NOTIFICATION
    // Used for main supplier employee and invited employees
    // =========================================================

    private void sendActivationNotification(
            Employee employee,
            Employee inviter,
            String activationToken
    ) {

        Company company = employee.getCompany();

        // Email context
        Map<String, String> emailMap = new HashMap<>();

        emailMap.put("employeeName", employee.getFirstName());
        emailMap.put("employeeEmail", employee.getEmailAddress());
        emailMap.put("companyName", company.getCompanyName());
        emailMap.put("employeeCode", employee.getEmployeeCode());
        emailMap.put("designation", employee.getDesignation());
        emailMap.put("department", employee.getDepartment());

        String managerName = "";

        if (employee.getManager() != null) {
            managerName =
                    employee.getManager().getFirstName()
                            + " "
                            + employee.getManager().getLastName();
        }

        emailMap.put("managerName", managerName);
        String activationUrl = applicationBaseUrl
                + "/activate?token="
                + java.net.URLEncoder.encode(
                activationToken,
                java.nio.charset.StandardCharsets.UTF_8
        );

        emailMap.put("activationUrl", activationUrl);
        emailMap.put(
                "currentYear",
                String.valueOf(java.time.Year.now().getValue())
        );

        // Subject context
        Map<String, String> subjectMap = new HashMap<>();

        subjectMap.put(
                "companyName",
                company.getCompanyName()
        );

        // Save notification context
        NotificationContext notificationContext =
                new NotificationContext(
                        SystemUtility.generateId("NOTIFICATION-CONTEXT"),
                        new HashMap<>(subjectMap),
                        new HashMap<>(emailMap),
                        new HashMap<>(),
                        new HashMap<>()
                );

        notificationContext =
                notificationContextRepo.save(notificationContext);

        // Fetch dedicated employee invitation template
        NotificationTemplate notificationTemplate =
                notificationService.fetchTemplateById(
                        NotificationTemplateConstant.INVITE_EMPLOYEE_TEMPLATE_ID
                );

        if (notificationTemplate == null) {
            throw new IllegalStateException(
                    "Employee invitation template not found"
            );
        }

        // Create notification
        Notification notification = new Notification();

        notification.setNotificationId(
                SystemUtility.generateId("NOTIFICATION")
        );

        notification.setToUsers(List.of(employee));

        notification.setCcUsers(new ArrayList<>());

        List<User> bccUsers = new ArrayList<>();

        if (inviter != null) {
            bccUsers.add(inviter);
        }

        notification.setBccUsers(bccUsers);

        notification.setNotificationContext(notificationContext);
        notification.setNotificationTemplate(notificationTemplate);

        notification.setEmail(true);
        notification.setSMS(false);
        notification.setWhatsApp(false);

        notification.setSMSDelivered(true);
        notification.setWhatsAppDelivered(true);

        notification.setNotificationPriority(
                NotificationPriority.URGENT
        );

        notification.setNotificationStatus(
                NotificationStatus.DRAFT
        );

        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());

        String createdBy = inviter != null
                ? inviter.getEmailAddress()
                : employee.getEmailAddress();

        notification.setCreatedBy(createdBy);
        notification.setUpdatedBy(createdBy);

        notificationService.sendNotification(notification);
    }

    // =========================================================
    // MANAGER ASSIGNMENT
    // =========================================================

    public Employee assignManager(
            UUID employeeSysId,
            AssignEmployeeManagerRequest request
    ) {

        Employee employee = employeeRepo.findById(employeeSysId)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found")
                );

        Employee manager = employeeRepo.findById(request.getManagerSysId())
                .orElseThrow(() ->
                        new RuntimeException("Manager not found")
                );

        if (employee.getSysId().equals(manager.getSysId())) {
            throw new RuntimeException(
                    "Employee cannot be their own manager"
            );
        }

        if (!employee.getCompany().getSysId()
                .equals(manager.getCompany().getSysId())) {

            throw new RuntimeException(
                    "Employee and manager must belong to the same company"
            );
        }

        if (manager.getUserStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException(
                    "Manager account is not active"
            );
        }

        boolean canApprove = manager.getRoles().stream()
                .filter(role -> role != null && role.getOperations() != null)
                .flatMap(role -> role.getOperations().stream())
                .anyMatch(operation ->
                        "APPROVE_PURCHASE_REQUEST".equals(
                                operation.getOperationName()
                        )
                );

        if (!canApprove) {
            throw new RuntimeException(
                    "Manager does not have APPROVE_PURCHASE_REQUEST permission"
            );
        }

        employee.setManager(manager);

        return employeeRepo.save(employee);
    }

    public Employee getEmployeeBySysId(UUID sysId) {
        return employeeRepo.findById(sysId).orElse(null);
    }

    public Employee getEmployeeByCode(String employeeCode) {
        return employeeRepo.findByEmployeeCode(employeeCode);
    }

    // =========================================================
    // PASSWORD RESET
    // =========================================================

    public void resetSupplierPassword(
            String email,
            String newPassword
    ) {

        Employee employee = employeeRepo.findByWorkEmail(email);

        if (employee == null) {
            throw new RuntimeException(
                    "Supplier employee not found"
            );
        }

        employee.setPasswordHash(
                passwordEncoder.encode(newPassword)
        );

        employeeRepo.save(employee);
    }

    // =========================================================
    // ACTIVATION TOKEN GENERATION
    // =========================================================

    private String generateActivationToken() {

        byte[] bytes = new byte[32];

        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hashToken(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(
                            java.nio.charset.StandardCharsets.UTF_8
                    )
            );

            return java.util.HexFormat.of().formatHex(hash);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Unable to hash activation token",
                    e
            );
        }
    }

    @Transactional
    public String createActivationToken(Employee employee) {

        // Remove previous unused activation tokens
        accountActivationRepo
                .findByEmployeeAndConsumedAtIsNull(employee)
                .forEach(accountActivationRepo::delete);

        // Generate raw token
        String rawToken = generateActivationToken();

        // Store only token hash
        AccountActivation activation = new AccountActivation();

        activation.setEmployee(employee);
        activation.setTokenHash(hashToken(rawToken));
        activation.setCreatedAt(Instant.now());

        activation.setExpiresAt(
                Instant.now().plus(Duration.ofHours(24))
        );

        accountActivationRepo.save(activation);

        // Raw token is used only in the notification
        return rawToken;
    }

    // =========================================================
    // EMPLOYEE ACTIVATION
    // =========================================================

    @Transactional
    public void activateEmployee(ActivateEmployeeRequest request) {

        String tokenHash = hashToken(request.getToken());

        AccountActivation activation =
                accountActivationRepo.findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid activation token"
                                )
                        );

        Instant now = Instant.now();

        if (activation.getConsumedAt() != null) {
            throw new IllegalArgumentException(
                    "Activation token has already been used"
            );
        }

        if (activation.getExpiresAt().isBefore(now)) {
            throw new IllegalArgumentException(
                    "Activation token has expired"
            );
        }

        Employee employee = activation.getEmployee();

        if (employee.isEmailVerified()) {
            throw new IllegalArgumentException(
                    "Employee is already activated"
            );
        }

        // Encode password before saving
        employee.setPasswordHash(
                passwordEncoder.encode(request.getNewPassword())
        );

        employee.setEmailVerified(true);
        employee.setUserStatus(UserStatus.ACTIVE);

        employeeRepo.save(employee);

        // Mark token as consumed
        activation.setConsumedAt(now);

        accountActivationRepo.save(activation);
    }
}