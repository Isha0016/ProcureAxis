package com.fsc.supply_chain_common_operations.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsc.supply_chain_common_operations.enums.NotificationPriority;
import com.fsc.supply_chain_common_operations.enums.NotificationStatus;
import com.fsc.supply_chain_common_operations.models.Notification;
import com.fsc.supply_chain_common_operations.models.User;
import com.fsc.supply_chain_common_operations.repositories.NotificationRepo;
import com.fsc.supply_chain_common_operations.utilities.SystemUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailHandlerImpl implements NotificationHandler {

    private final NotificationRepo notificationRepo;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${fsc.email.apps-script-url:}")
    private String appsScriptUrl;

    @Value("${fsc.email.apps-script-api-key:}")
    private String appsScriptApiKey;

    public EmailHandlerImpl(
            NotificationRepo notificationRepo,
            ObjectMapper objectMapper) {

        this.notificationRepo = notificationRepo;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    @Override
    public Notification send(Notification notification) {

        log.info("Email notification processing started");

        try {
            if (notification.getNotificationPriority().equals(NotificationPriority.URGENT)
                    && notification.getNotificationStatus().equals(NotificationStatus.DRAFT)) {

                log.info("Processing URGENT email notification");

                this.sendEmail(notification);
                notification.setEmailFailed(false);
                notification.setEmailDelivered(true);

                log.info("URGENT email notification sent successfully");

            } else if (notification.getNotificationPriority().equals(NotificationPriority.NORMAL)
                    && notification.getNotificationStatus().equals(NotificationStatus.IN_QUEUE)) {

                log.info("Processing queued NORMAL email notification");

                this.sendEmail(notification);
                notification.setEmailFailed(false);
                notification.setEmailDelivered(true);

                log.info("Queued email notification sent successfully");

            } else if (notification.isEmailFailed()) {

                log.info("Retrying previously failed email notification");

                this.sendEmail(notification);
                notification.setEmailFailed(false);
                notification.setEmailDelivered(true);

                log.info("Email notification retry successful");

            } else {

                log.info("NORMAL notification received. Moving notification to IN_QUEUE");

                notification.setNotificationStatus(NotificationStatus.IN_QUEUE);
            }

        } catch (Exception e) {

            log.error("Unable to send email notification. Error: {}", e.getMessage(), e);

            String previousFailure = notification.getFailureMessage() == null
                    ? ""
                    : notification.getFailureMessage();

            notification.setFailureMessage(
                    previousFailure + " [EMAIL] - " + e.getMessage()
            );

            notification.setEmailFailed(true);
            notification.setEmailDelivered(false);
            notification.setNotificationStatus(NotificationStatus.FAILED);
        }

        log.info("Saving notification with status={}",
                notification.getNotificationStatus());

        notification.setUpdatedAt(LocalDateTime.now());
        notification.setUpdatedBy("system");

        this.notificationRepo.save(notification);

        log.info("Email notification processing completed");

        return notification;
    }

    private void sendEmail(Notification notification) throws Exception {

        log.info("Preparing email notification through Google Apps Script");

        if (appsScriptUrl == null || appsScriptUrl.isBlank()
                || appsScriptApiKey == null || appsScriptApiKey.isBlank()) {
            throw new IllegalStateException(
                    "Google Apps Script URL or API key is not configured"
            );
        }

        List<String> toUsers = notification.getToUsers()
                .stream()
                .map(User::getEmailAddress)
                .toList();

        List<String> ccUsers = notification.getCcUsers()
                .stream()
                .map(User::getEmailAddress)
                .toList();

        List<String> bccUsers = notification.getBccUsers()
                .stream()
                .map(User::getEmailAddress)
                .toList();

        if (toUsers.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one recipient email address is required"
            );
        }

        String emailBody = notification.getNotificationTemplate().getEmailTemplate();
        String subjectLine = notification.getNotificationTemplate().getSubjectLine();

        emailBody = SystemUtility.replaceTemplateVariableWithValues(
                emailBody,
                notification.getNotificationContext().getEmailMap()
        );

        subjectLine = SystemUtility.replaceTemplateVariableWithValues(
                subjectLine,
                notification.getNotificationContext().getSubjectLineMap()
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("apiKey", appsScriptApiKey);
        requestBody.put("to", String.join(",", toUsers));
        requestBody.put("cc", String.join(",", ccUsers));
        requestBody.put("bcc", String.join(",", bccUsers));
        requestBody.put("subject", subjectLine);
        requestBody.put("body", emailBody);
        requestBody.put("htmlBody", true);

        String jsonPayload = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(appsScriptUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        log.info("Sending email through Google Apps Script. To={}, CC={}, BCC={}",
                toUsers.size(), ccUsers.size(), bccUsers.size());

        RetryService.retry(() -> {
            try {
                HttpResponse<String> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException(
                            "Google Apps Script returned HTTP status "
                                    + response.statusCode()
                    );
                }

                Map<?, ?> responseData = objectMapper.readValue(
                        response.body(),
                        Map.class
                );

                if (!Boolean.TRUE.equals(responseData.get("success"))) {
                    throw new IllegalStateException(
                            "Google Apps Script email failed: "
                                    + responseData.get("message")
                    );
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        log.info("Google Apps Script accepted the email request successfully");
    }
}