package com.payprosys.repository;

import com.payprosys.entity.CorporateWorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CorporateWorkflowStepRepository extends JpaRepository<CorporateWorkflowStep, UUID> {

    List<CorporateWorkflowStep> findByCorporate_IdOrderByStepLevelAsc(UUID corporateId);

    long countByCorporate_Id(UUID corporateId);

    Optional<CorporateWorkflowStep> findByCorporate_IdAndStepLevel(UUID corporateId, int stepLevel);
}
