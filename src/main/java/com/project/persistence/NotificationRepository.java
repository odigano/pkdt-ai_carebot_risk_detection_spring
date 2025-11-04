package com.project.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.domain.notification.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long>{
	List<Notification> findByRecipientUsernameOrderByCreatedAtDesc(String username);
	
	@Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.username = :username AND n.isRead = false")
    void markAllAsReadByUsername(@Param("username") String username);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.recipient.username = :username")
    void deleteAllByUsername(@Param("username") String username);
}
