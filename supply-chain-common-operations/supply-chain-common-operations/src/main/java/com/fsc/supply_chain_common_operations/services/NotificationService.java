package com.fsc.supply_chain_common_operations.services;

import com.fsc.supply_chain_common_operations.enums.NotificationStatus;
import com.fsc.supply_chain_common_operations.models.Notification;
import com.fsc.supply_chain_common_operations.models.NotificationTemplate;
import com.fsc.supply_chain_common_operations.repositories.NotificationRepo;
import com.fsc.supply_chain_common_operations.repositories.NotificationTemplateRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import org.hibernate.Hibernate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    private EmailHandlerImpl emailHandler;
    private NotificationRepo notificationRepo;
    private NotificationTemplateRepo  notificationTemplateRepo;

    @Autowired
    public NotificationService(EmailHandlerImpl emailHandler,
                               NotificationRepo notificationRepo,
                               NotificationTemplateRepo notificationTemplateRepo){
        this.emailHandler = emailHandler;
        this.notificationTemplateRepo = notificationTemplateRepo;
        this.notificationRepo = notificationRepo;
    }
    public Notification saveNotification(Notification notification) {
        return notificationRepo.save(notification);
    }
    @Transactional
    public Notification sendNotificationById(UUID notificationId) {

        Notification notification = notificationRepo
                .findNotificationWithDetails(notificationId)
                .orElseThrow(() ->
                        new RuntimeException("Notification not found: " + notificationId)
                );

        return sendNotification(notification);
    }
    /**
     * Whenever any service wants to send the nottification they will call
     * this sendNotification function and this function needs just notification object.
     * In whatever mode you want to send notification you just need to toggle on the flag for that notification type.
     * @param notification
     */
    public Notification sendNotification(Notification notification) {

        log.info("Notification process started");

        log.info("Saving notification");
        notificationRepo.save(notification);
        log.info("Notification saved successfully");

        if (notification.isEmail()) {
            log.info("Sending email notification");
            emailHandler.send(notification);
            log.info("Email notification sent successfully");
        }

        if (notification.isSMS()) {
            log.info("Sending SMS notification");
            notification.setSMSDelivered(true);
            // SMSService
        }

        if (notification.isWhatsApp()) {
            log.info("Sending WhatsApp notification");
            notification.setWhatsAppDelivered(true);
            // WhatsAppService
        }

        if(notification.isEmailDelivered() && notification.isSMSDelivered() && notification.isWhatsAppDelivered()){
            notification.setNotificationStatus(NotificationStatus.DELIVERED);
        }

        log.info("Notification process completed successfully");

        return notification;
    }


    @Transactional
    public List<Notification> fetchNotificationByStatus(
            NotificationStatus notificationStatus) {

        List<Notification> notifications =
                notificationRepo.findByNotificationStatus(notificationStatus);

        for (Notification notification : notifications) {

            Hibernate.initialize(notification.getToUsers());
            Hibernate.initialize(notification.getCcUsers());
            Hibernate.initialize(notification.getBccUsers());

            Hibernate.initialize(notification.getNotificationTemplate());
            Hibernate.initialize(notification.getNotificationContext());
        }

        return notifications;
    }


    public NotificationTemplate fetchTemplateById(UUID notificationTemplateId){
        return notificationTemplateRepo.findById(notificationTemplateId).orElse(null);
    }

}
