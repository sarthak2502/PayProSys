package com.payprosys.repository;

import com.payprosys.entity.CorporateUserReviewLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CorporateUserReviewLevelRepository extends JpaRepository<CorporateUserReviewLevel, UUID> {

    List<CorporateUserReviewLevel> findByCorporate_IdOrderByReviewLevelAsc(UUID corporateId);

    List<CorporateUserReviewLevel> findByCorporate_IdAndUser_Id(UUID corporateId, UUID userId);

    long countByCorporate_Id(UUID corporateId);

    Optional<CorporateUserReviewLevel> findByCorporate_IdAndUser_IdAndReviewLevel(UUID corporateId, UUID userId, int reviewLevel);
}
