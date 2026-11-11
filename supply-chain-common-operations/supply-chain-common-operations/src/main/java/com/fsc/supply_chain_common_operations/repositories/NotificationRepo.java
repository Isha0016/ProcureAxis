package com.fsc.supply_chain_common_operations.repositories;

import com.fsc.supply_chain_common_operations.enums.NotificationStatus;
import com.fsc.supply_chain_common_operations.models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepo extends JpaRepository<Notification, UUID> {

    @Query("SELECT n FROM Notification n WHERE n.sysId = :id")
    Optional<Notification> findNotificationWithDetails(
            @Param("id") UUID id
    );

    List<Notification> findByNotificationStatus(NotificationStatus notificationStatus);
}
