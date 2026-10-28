package com.campusbid.backend.repository;

import com.campusbid.backend.model.Notification;
import com.campusbid.backend.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId);

    long countByRecipientIdAndReadFalse(String recipientId);

    Optional<Notification> findByIdAndRecipientId(Long id, String recipientId);

    boolean existsByRecipientIdAndRelatedListingIdAndType(
        String recipientId,
        Long relatedListingId,
        NotificationType type
    );

    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :now WHERE n.recipientId = :recipientId AND n.read = false")
    int markAllAsRead(@Param("recipientId") String recipientId, @Param("now") LocalDateTime now);
}
