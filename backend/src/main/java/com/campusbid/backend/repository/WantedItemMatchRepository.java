package com.campusbid.backend.repository;

import com.campusbid.backend.model.WantedItemMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface WantedItemMatchRepository extends JpaRepository<WantedItemMatch, Long> {

    boolean existsByWantedItemIdAndListingId(Long wantedItemId, Long listingId);

    List<WantedItemMatch> findByWantedItemIdOrderByMatchedAtDesc(Long wantedItemId);

    List<WantedItemMatch> findByWantedItemIdInOrderByMatchedAtDesc(Collection<Long> wantedItemIds);

    long countByWantedItemId(Long wantedItemId);

    void deleteByWantedItemId(Long wantedItemId);
}
