package com.campusbid.backend.repository;

import com.campusbid.backend.model.Listing;
import com.campusbid.backend.model.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findByStatusOrderByCreatedAtDesc(ListingStatus status);

    List<Listing> findAllByOrderByCreatedAtDesc();

    List<Listing> findBySellerIdOrderByCreatedAtDesc(String sellerId);

    List<Listing> findByCategoryIgnoreCaseAndStatusOrderByCreatedAtDesc(String category, ListingStatus status);

    @Query("SELECT l FROM Listing l WHERE (:status IS NULL OR l.status = :status) " +
           "AND (:category IS NULL OR LOWER(l.category) = LOWER(:category)) " +
           "AND (:keyword IS NULL OR LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(l.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY l.createdAt DESC")
    List<Listing> searchListings(
            @Param("status") ListingStatus status,
            @Param("category") String category,
            @Param("keyword") String keyword
    );
}
