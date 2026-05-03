package com.payprosys.service;

import com.payprosys.entity.Bank;
import com.payprosys.entity.BankWorkflowStep;
import com.payprosys.entity.Corporate;
import com.payprosys.entity.CorporateWorkflowStep;
import com.payprosys.repository.BankWorkflowStepRepository;
import com.payprosys.repository.CorporateWorkflowStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures each corporate / bank has default L1–L3 workflow rows (POC). Flyway seeds existing rows;
 * this covers banks and corporates created later via the API.
 */
@Service
@RequiredArgsConstructor
public class WorkflowStepBootstrapService {

    private static final int DEFAULT_LEVELS = 3;

    private final CorporateWorkflowStepRepository corporateWorkflowStepRepository;
    private final BankWorkflowStepRepository bankWorkflowStepRepository;

    @Transactional
    public void ensureDefaultCorporateSteps(Corporate corporate) {
        if (corporate == null || corporate.getId() == null) {
            return;
        }
        if (corporateWorkflowStepRepository.countByCorporate_Id(corporate.getId()) > 0) {
            return;
        }
        for (int level = 1; level <= DEFAULT_LEVELS; level++) {
            corporateWorkflowStepRepository.save(CorporateWorkflowStep.builder()
                    .corporate(corporate)
                    .stepLevel(level)
                    .label("Level " + level)
                    .build());
        }
    }

    @Transactional
    public void ensureDefaultBankSteps(Bank bank) {
        if (bank == null || bank.getId() == null) {
            return;
        }
        if (bankWorkflowStepRepository.countByBank_Id(bank.getId()) > 0) {
            return;
        }
        for (int level = 1; level <= DEFAULT_LEVELS; level++) {
            bankWorkflowStepRepository.save(BankWorkflowStep.builder()
                    .bank(bank)
                    .stepLevel(level)
                    .label("Level " + level)
                    .build());
        }
    }
}
