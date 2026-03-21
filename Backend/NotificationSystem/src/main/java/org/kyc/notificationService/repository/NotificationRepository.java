package org.kyc.notificationService.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.kyc.notificationService.model.Notification;
import org.kyc.notificationService.model.NotificationStatus;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByUserIdAndStatus(String userId, NotificationStatus status);

    List<Notification> findByUserIdAndStatusIn(String userId, List<NotificationStatus> statuses);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'SENT' WHERE n.id = :id AND n.status = 'PENDING'")
    int updateStatusToSentIfPending(@Param("id") String id);
}
