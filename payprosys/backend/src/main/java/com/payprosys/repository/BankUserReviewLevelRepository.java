package com.payprosys.repository;

import com.payprosys.entity.BankUserReviewLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankUserReviewLevelRepository extends JpaRepository<BankUserReviewLevel, UUID> {

    List<BankUserReviewLevel> findByBank_IdOrderByReviewLevelAsc(UUID bankId);

    List<BankUserReviewLevel> findByBank_IdAndUser_Id(UUID bankId, UUID userId);

    long countByBank_Id(UUID bankId);

    Optional<BankUserReviewLevel> findByBank_IdAndUser_IdAndReviewLevel(UUID bankId, UUID userId, int reviewLevel);
}
