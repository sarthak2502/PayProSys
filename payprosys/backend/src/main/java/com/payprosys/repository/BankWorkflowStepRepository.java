package com.payprosys.repository;

import com.payprosys.entity.BankWorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankWorkflowStepRepository extends JpaRepository<BankWorkflowStep, UUID> {

    List<BankWorkflowStep> findByBank_IdOrderByStepLevelAsc(UUID bankId);

    long countByBank_Id(UUID bankId);

    Optional<BankWorkflowStep> findByBank_IdAndStepLevel(UUID bankId, int stepLevel);
}
