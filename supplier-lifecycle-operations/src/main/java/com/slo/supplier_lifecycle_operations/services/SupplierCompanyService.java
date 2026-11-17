package com.slo.supplier_lifecycle_operations.services;

import java.util.UUID;
import com.fsc.supply_chain_common_operations.constants.NotificationTemplateConstant;
import com.fsc.supply_chain_common_operations.clients.GeminiClient;
import com.fsc.supply_chain_common_operations.enums.CompanyType;
import com.fsc.supply_chain_common_operations.enums.NotificationPriority;
import com.fsc.supply_chain_common_operations.enums.NotificationStatus;
import com.fsc.supply_chain_common_operations.models.*;
import com.fsc.supply_chain_common_operations.repositories.NotificationContextRepo;
import com.fsc.supply_chain_common_operations.repositories.NotificationTemplateRepo;
import com.fsc.supply_chain_common_operations.repositories.SupplierCompanyRepo;
import com.fsc.supply_chain_common_operations.services.EmployeeService;
import com.fsc.supply_chain_common_operations.services.NotificationService;
import com.fsc.supply_chain_common_operations.utilities.SystemUtility;
import com.slo.supplier_lifecycle_operations.dtos.requests.SupplierCompanyRegistrationRequest;
import com.slo.supplier_lifecycle_operations.transformers.RequestTransformer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fsc.supply_chain_common_operations.exceptions.DuplicateRecordException;
import com.fsc.supply_chain_common_operations.enums.CompanyStatus;
import com.fsc.supply_chain_common_operations.exceptions.RecordDoesNotExistException;
import org.springframework.transaction.annotation.Transactional;
import com.slo.supplier_lifecycle_operations.dtos.requests.SupplierCompanyUpdateRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import io.jsonwebtoken.Claims;
import com.fsc.supply_chain_common_operations.enums.CompanyType;
import java.util.UUID;
import com.fsc.supply_chain_common_operations.services.AuthService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
public class SupplierCompanyService {

    private final  SupplierCompanyRepo supplierCompanyRepo;
    private final EmployeeService employeeService;
    private final NotificationTemplateRepo notificationTemplateRepo;
    private final NotificationContextRepo notificationContextRepo;
    private final ExecutorService executorService;
    private final NotificationService notificationService;
    private final AuthService authService;


    @Autowired
    public SupplierCompanyService(SupplierCompanyRepo supplierCompanyRepo,
                                  EmployeeService employeeService,
                                  NotificationTemplateRepo notificationTemplateRepo,
                                  NotificationService notificationService,
                                  NotificationContextRepo notificationContextRepo,
                                  ExecutorService executorService,AuthService authService
                                  ){
        this.supplierCompanyRepo = supplierCompanyRepo;
        this.employeeService = employeeService;
        this.notificationTemplateRepo = notificationTemplateRepo;
        this.notificationService = notificationService;
        this.executorService = executorService;
        this.notificationContextRepo = notificationContextRepo;
        this.authService =  authService;
    }

    public SupplierCompany registerSupplierCompany(
            SupplierCompanyRegistrationRequest supplierCompanyRegistrationRequest) {

        log.info("Starting supplier company registration. companyName={}",
                supplierCompanyRegistrationRequest.getCompanyName());

        // Check for duplicate email
        if (supplierCompanyRepo.existsByEmailAddressIgnoreCase(
                supplierCompanyRegistrationRequest.getEmailAddress())) {
            throw new DuplicateRecordException(
                    "A supplier company with this email address already exists.");
        }

        // Check for duplicate company name
        if (supplierCompanyRepo.existsByCompanyNameIgnoreCase(
                supplierCompanyRegistrationRequest.getCompanyName())) {
            throw new DuplicateRecordException(
                    "A supplier company with this name already exists.");
        }

        // Map registration request to the Supplier Company model
        SupplierCompany supplierCompany =
                RequestTransformer.mapRegistrationRequestToSupplierCompany(
                        supplierCompanyRegistrationRequest);
        supplierCompany.setCompanyType(CompanyType.SUPPLIER_COMPANY);

        String feedback = getSupplierCompanyFeedback(supplierCompany);

        log.debug("Successfully mapped registration request to SupplierCompany. companyName={}",
                supplierCompany.getCompanyName());

        // Save supplier company
        supplierCompany = this.saveOrUpdate(supplierCompany);
        supplierCompany.setAiFeedback(feedback);


        log.info("Supplier company saved successfully. companyName={}, companyId={}",
                supplierCompany.getCompanyName(),
                supplierCompany.getSysId());

        // Create main/admin user for the supplier company
        log.info("Creating main employee for supplier company. companyId={}",
                supplierCompany.getSysId());

        EmployeeService.MaintUserCreationResult userCreation =
                employeeService.createMaintUserForCompanyWithCredentials(
                        supplierCompany,
                        CompanyType.SUPPLIER_COMPANY
                );

        Employee maintEmployee = userCreation.employee();
        String temporaryPassword = userCreation.temporaryPassword();

        log.info("Main employee created successfully. companyId={}, employeeId={}",
                supplierCompany.getSysId(),
                maintEmployee.getSysId());

        log.info("Supplier company registration completed successfully. companyId={}",
                supplierCompany.getSysId());

        // 1. Decide which notification you want to send.
        NotificationTemplate notificationTemplate = notificationTemplateRepo.findById(NotificationTemplateConstant.COMPANY_REGISTRATION_TEMPLATE_ID).orElse(null);
        // 2. As we got the template we need to create the context for the template
        HashMap<String, String> emailMap = new HashMap<>();

        emailMap.put("maintUserName", maintEmployee.getFirstName());
        emailMap.put("companyName", supplierCompany.getCompanyName());
        emailMap.put("maintUserEmail", maintEmployee.getEmailAddress());
        emailMap.put("temporaryPassword", temporaryPassword);
        emailMap.put("applicationUrl", "https://www.google.com");
        emailMap.put("currentYear", String.valueOf(LocalDate.now().getYear()));

        HashMap<String, String> subjectLineMap = new HashMap<>();

        subjectLineMap.put("companyName", supplierCompany.getCompanyName());

        NotificationContext notificationContext = new NotificationContext();
        notificationContext.setSubjectLineMap(subjectLineMap);
        notificationContext.setEmailMap(emailMap);
        notificationContext.setSmsMap(new HashMap<>());
        notificationContext.setWhatsAppMap(new HashMap<>());
        notificationContext.setContextId(SystemUtility.generateId("NOTIFICATION-CONTEXT"));
        notificationContext.setCreatedBy("system");
        notificationContext.setUpdatedBy("system");
        notificationContext.setCreatedAt(LocalDateTime.now());
        notificationContext.setUpdatedAt(LocalDateTime.now());

        notificationContextRepo.save(notificationContext);

        // 3. Create Notification object

        Notification notification = Notification.builder()
                .notificationContext(notificationContext)
                .notificationTemplate(notificationTemplate)
                .notificationId(SystemUtility.generateId("NOTIFICATION"))
                .notificationPriority(NotificationPriority.URGENT)
                .notificationStatus(NotificationStatus.DRAFT)
                .bccUsers(new ArrayList<>())
                .ccUsers(new ArrayList<>())
                .isEmail(true)
                .isSMSDelivered(true)
                .isWhatsAppDelivered(true)
                .isSMS(false)
                .isWhatsApp(false)
                .toUsers(List.of(maintEmployee))
                .createdAt(LocalDateTime.now())
                .createdBy("system")
                .updatedAt(LocalDateTime.now())
                .updatedBy("system")
                .build();

        // Call NotificationService to send the notification

        // This send notification method will be slow - sendNotification is internally dependent on external calls
        // Externally Calling javaMailSender which will call gmail server to send email which itself take some time
        // smsService - Twillio will send smsNotification which will take time
        // Whatsappservice will call whatsappserver which will take its time

        // Save notification before starting asynchronous execution
        Notification savedNotification =
                notificationService.saveNotification(notification);

        // Get notification ID
        UUID notificationId = savedNotification.getSysId();

        // Send notification asynchronously using ID
        executorService.submit(() -> {
            notificationService.sendNotificationById(notificationId);
        });


        return supplierCompany;
    }



    public SupplierCompany saveOrUpdate(SupplierCompany supplierCompany){
        return this.supplierCompanyRepo.save(supplierCompany);
    }
    @Transactional
    public SupplierCompany startSupplierCompanyReview(UUID companyId) {

        SupplierCompany company = supplierCompanyRepo.findById(companyId)
                .orElseThrow(() -> new RecordDoesNotExistException(
                        "Supplier company not found: " + companyId));

        if (company.getStatus() != CompanyStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING suppliers can move to UNDER_REVIEW.");
        }

        company.setStatus(CompanyStatus.UNDER_REVIEW);

        return supplierCompanyRepo.save(company);
    }


    @Transactional
    public SupplierCompany approveSupplierCompany(UUID companyId) {

        SupplierCompany company = supplierCompanyRepo.findById(companyId)
                .orElseThrow(() -> new RecordDoesNotExistException(
                        "Supplier company not found: " + companyId));

        if (company.getStatus() != CompanyStatus.UNDER_REVIEW) {
            throw new IllegalStateException(
                    "Only suppliers UNDER_REVIEW can be approved.");
        }

        company.setStatus(CompanyStatus.ACTIVE);

        return supplierCompanyRepo.save(company);
    }
    @Transactional
    public SupplierCompany suspendSupplierCompany(UUID companyId) {

        SupplierCompany company = supplierCompanyRepo.findById(companyId)
                .orElseThrow(() -> new RecordDoesNotExistException(
                        "Supplier company not found: " + companyId));

        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only ACTIVE suppliers can be suspended.");
        }

        company.setStatus(CompanyStatus.SUSPENDED);

        return supplierCompanyRepo.save(company);
    }
    @Transactional
    public SupplierCompany deactivateSupplierCompany(UUID companyId) {

        SupplierCompany company = supplierCompanyRepo.findById(companyId)
                .orElseThrow(() -> new RecordDoesNotExistException(
                        "Supplier company not found: " + companyId));

        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only ACTIVE suppliers can be deactivated.");
        }

        company.setStatus(CompanyStatus.INACTIVE);

        return supplierCompanyRepo.save(company);
    }
    @Transactional
    public SupplierCompany reinstateSupplierCompany(UUID companyId) {

        SupplierCompany company = supplierCompanyRepo.findById(companyId)
                .orElseThrow(() -> new RecordDoesNotExistException(
                        "Supplier company not found: " + companyId));

        if (company.getStatus() != CompanyStatus.SUSPENDED &&
                company.getStatus() != CompanyStatus.INACTIVE) {

            throw new IllegalStateException(
                    "Only SUSPENDED or INACTIVE suppliers can be reinstated.");
        }

        // Reinstatement requires another internal review.
        company.setStatus(CompanyStatus.UNDER_REVIEW);

        return supplierCompanyRepo.save(company);
    }





    @Transactional
    public SupplierCompany rejectSupplierCompany(
            UUID companyId, String rejectionReason) {

        SupplierCompany company = supplierCompanyRepo.findById(companyId)
                .orElseThrow(() -> new RecordDoesNotExistException(
                        "Supplier company not found: " + companyId));

        if (company.getStatus() != CompanyStatus.UNDER_REVIEW) {
            throw new IllegalStateException(
                    "Only suppliers UNDER_REVIEW can be rejected.");
        }

        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new IllegalArgumentException(
                    "Rejection reason is required.");
        }

        company.setStatus(CompanyStatus.REJECTED);
        company.setRejectionReason(rejectionReason.trim());

        return supplierCompanyRepo.save(company);
    }

    @Transactional(readOnly = true)
    public SupplierCompany getSupplierCompanyById(
            UUID companyId, String token) {

        Claims claims = (Claims) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String authenticatedCompanyId =
                claims.get("companyId", String.class);

        // Supplier can access its own company.
        if (companyId.toString().equals(authenticatedCompanyId)) {
            return supplierCompanyRepo.findById(companyId)
                    .orElseThrow(() -> new RecordDoesNotExistException(
                            "Supplier company not found: " + companyId));
        }

        // Access to another company requires internal reviewer permission.
        authService.isUserHavingAccess(token, "APPROVE_SUPPLIER");

        return supplierCompanyRepo.findById(companyId)
                .orElseThrow(() -> new RecordDoesNotExistException(
                        "Supplier company not found: " + companyId));
    }

    @Transactional
    public SupplierCompany updateSupplierCompany(
            UUID companyId,
            SupplierCompanyUpdateRequest request) {
        Claims claims = (Claims) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String authenticatedCompanyId =
                claims.get("companyId", String.class);

        if (!companyId.toString().equals(authenticatedCompanyId)) {
            throw new AccessDeniedException(
                    "You are not authorized to update this company"
            );
        }

        SupplierCompany company = supplierCompanyRepo.findById(companyId)
                .orElseThrow(() -> new RecordDoesNotExistException(
                        "Supplier company not found: " + companyId));

        // Update common company fields
        if (request.getCompanyName() != null) {
            company.setCompanyName(request.getCompanyName());
        }

        if (request.getLegalName() != null) {
            company.setLegalName(request.getLegalName());
        }

        if (request.getGstNumber() != null) {
            company.setGstNumber(request.getGstNumber());
        }

        if (request.getPanNumber() != null) {
            company.setPanNumber(request.getPanNumber());
        }

        if (request.getWebsite() != null) {
            company.setWebsite(request.getWebsite());
        }

        if (request.getContactNumber() != null) {
            company.setContactNumber(request.getContactNumber());
        }

        // Update supplier-specific fields
        if (request.getSupplierCategory() != null) {
            company.setSupplierCategory(request.getSupplierCategory());
        }

        if (request.getLeadTimeDays() != null) {
            company.setLeadTimeDays(request.getLeadTimeDays());
        }

        if (request.getMinimumOrderValue() != null) {
            company.setMinimumOrderValue(request.getMinimumOrderValue());
        }

        return supplierCompanyRepo.save(company);
    }


    public String getSupplierCompanyFeedback(SupplierCompany supplierCompany) {

        String prompt =
                "Analyze the following supplier company details and provide comprehensive, " +
                        "constructive feedback about the supplier's overall performance, strengths, " +
                        "weaknesses, risks, and areas for improvement.\n\n" +
                        "Supplier Company Payload:\n" +
                        supplierCompany.toString();

        GeminiClient geminiClient = new GeminiClient();

        return geminiClient.getAIResponse(prompt);
    }

}
