package org.rbi.notificationService.repository;

import java.util.List;
import org.rbi.notificationService.model.Notification;
import org.rbi.notificationService.model.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByTargetIdAndStatus(String targetId, NotificationStatus status);

    List<Notification> findByTargetIdAndStatusIn(
            String targetId, List<NotificationStatus> statuses);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'SENT' WHERE n.id = :id AND n.status = 'PENDING'")
    int updateStatusToSentIfPending(@Param("id") String id);
}
