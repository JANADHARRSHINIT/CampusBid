package com.campusbid.backend.repository;

import com.campusbid.backend.model.WantedItem;
import com.campusbid.backend.model.WantedItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WantedItemRepository extends JpaRepository<WantedItem, Long> {

    List<WantedItem> findByStudentIdOrderByCreatedAtDesc(String studentId);

    List<WantedItem> findByStudentEmailOrderByCreatedAtDesc(String studentEmail);

    List<WantedItem> findByStudentIdOrStudentEmailOrderByCreatedAtDesc(String studentId, String studentEmail);

    List<WantedItem> findByStatus(WantedItemStatus status);

    Optional<WantedItem> findByIdAndStudentId(Long id, String studentId);

    long countByStudentIdAndStatus(String studentId, WantedItemStatus status);
}
