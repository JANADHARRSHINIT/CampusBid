package com.campusbid.backend.repository;

import com.campusbid.backend.model.ItemCondition;
import com.campusbid.backend.model.Listing;
import com.campusbid.backend.model.ListingStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findByStatusOrderByCreatedAtDesc(ListingStatus status);

    List<Listing> findAllByOrderByCreatedAtDesc();

    List<Listing> findBySellerIdOrderByCreatedAtDesc(String sellerId);

    List<Listing> findByCategoryIgnoreCaseAndStatusOrderByCreatedAtDesc(String category, ListingStatus status);

    @Query("SELECT l FROM Listing l WHERE (:status IS NULL OR l.status = :status) " +
           "AND (:category IS NULL OR :category = '' OR LOWER(l.category) = LOWER(:category)) " +
           "AND (:condition IS NULL OR l.condition = :condition) " +
           "AND (:minPrice IS NULL OR l.currentPrice >= :minPrice) " +
           "AND (:maxPrice IS NULL OR l.currentPrice <= :maxPrice) " +
           "AND (:closingSoonThreshold IS NULL OR (l.closingTime <= :closingSoonThreshold AND l.closingTime > :now)) " +
           "AND (l.status != com.campusbid.backend.model.ListingStatus.ACTIVE OR l.closingTime > :now) " +
           "AND (:keyword IS NULL OR :keyword = '' OR (LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(l.description) LIKE LOWER(CONCAT('%', :keyword, '%'))))")
    List<Listing> searchWithFilters(
            @Param("status") ListingStatus status,
            @Param("category") String category,
            @Param("condition") ItemCondition condition,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("closingSoonThreshold") LocalDateTime closingSoonThreshold,
            @Param("now") LocalDateTime now,
            @Param("keyword") String keyword,
            Sort sort
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Listing l SET l.status = :endedStatus WHERE l.status = :activeStatus AND l.closingTime <= :now")
    int closeExpiredListings(
            @Param("activeStatus") ListingStatus activeStatus,
            @Param("endedStatus") ListingStatus endedStatus,
            @Param("now") LocalDateTime now
    );
}
