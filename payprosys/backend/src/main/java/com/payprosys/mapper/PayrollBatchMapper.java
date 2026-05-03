package com.payprosys.mapper;

import com.payprosys.dto.PayrollBatchDto;
import com.payprosys.entity.BankFlowState;
import com.payprosys.entity.CorporateFlowState;
import com.payprosys.entity.PaymentBatchKind;
import com.payprosys.entity.PayrollBatch;
import com.payprosys.entity.PayrollBatchStatus;
import org.springframework.stereotype.Component;

@Component
public class PayrollBatchMapper {

    public PayrollBatchDto toDto(PayrollBatch entity) {
        if (entity == null) return null;
        return PayrollBatchDto.builder()
                .id(entity.getId())
                .corporateId(entity.getCorporate().getId())
                .uploadedById(entity.getUploadedBy().getId())
                .totalRecords(entity.getTotalRecords())
                .totalAmount(entity.getTotalAmount())
                .fileName(entity.getFileName())
                .yearMonth(entity.getYearMonth())
                .createdAt(entity.getCreatedAt())
                .batchStatus(entity.getBatchStatus() != null ? entity.getBatchStatus().name() : null)
                .batchStatusLabel(computeBatchStatusLabel(entity))
                .workflowListSummary(computeWorkflowListSummary(entity))
                .corporateFlowState(entity.getCorporateFlowState() != null ? entity.getCorporateFlowState().name() : null)
                .bankFlowState(entity.getBankFlowState() != null ? entity.getBankFlowState().name() : null)
                .currentCorporateReviewLevel(entity.getCurrentCorporateReviewLevel())
                .currentBankReviewLevel(entity.getCurrentBankReviewLevel())
                .remarksForBank(entity.getRemarksForBank())
                .corporateName(entity.getCorporate() != null ? entity.getCorporate().getName() : null)
                .paymentBatchKind(entity.getPaymentBatchKind() != null ? entity.getPaymentBatchKind().name() : null)
                .paymentBatchKindLabel(paymentBatchKindLabel(entity.getPaymentBatchKind()))
                .build();
    }

    /**
     * Once the batch is with the bank, treat as submitted even if legacy rows had inconsistent {@code batch_status}.
     */
    private static String computeBatchStatusLabel(PayrollBatch e) {
        if (e.getBatchStatus() == PayrollBatchStatus.COMPLETED) {
            return "Completed";
        }
        if (e.getBankFlowState() == BankFlowState.BANK_PROCESS_PAYMENT) {
            return "Completed";
        }
        CorporateFlowState corp = e.getCorporateFlowState();
        if (corp == CorporateFlowState.CORP_SENT_TO_BANK) {
            return "SUBMITTED";
        }
        PayrollBatchStatus st = e.getBatchStatus();
        if (st == PayrollBatchStatus.SUBMITTED) {
            return "SUBMITTED";
        }
        if (corp == CorporateFlowState.CORP_APPROVED_HOLD) {
            return "APPROVED_HOLD";
        }
        if (st == PayrollBatchStatus.PENDING) {
            if (corp == CorporateFlowState.CORP_NEW && e.getCurrentCorporateReviewLevel() == null) {
                return "DRAFT";
            }
            if (corp == CorporateFlowState.CORP_REJECTED) {
                return "REJECTED";
            }
            return "IN_CORPORATE_REVIEW";
        }
        return st != null ? st.name() : null;
    }

    /**
     * Plain-English status and actor for payroll inbox/history (middle dot separates the two parts).
     */
    private static String computeWorkflowListSummary(PayrollBatch e) {
        PayrollBatchStatus st = e.getBatchStatus();
        CorporateFlowState cf = e.getCorporateFlowState();
        BankFlowState bf = e.getBankFlowState();
        Integer ccl = e.getCurrentCorporateReviewLevel();
        Integer bcl = e.getCurrentBankReviewLevel();

        if (cf == CorporateFlowState.CORP_REJECTED) {
            return "Rejected · —";
        }
        if (st == PayrollBatchStatus.COMPLETED || bf == BankFlowState.BANK_PROCESS_PAYMENT) {
            return "Completed · Bank";
        }
        if (cf == CorporateFlowState.CORP_SENT_TO_BANK && bf != null) {
            if (bf == BankFlowState.BANK_APPROVED) {
                return "Bank approved — awaiting process payment · Bank (processor)";
            }
            if (bf == BankFlowState.BANK_SENT_TO_CORPORATE) {
                int lvl = ccl != null ? ccl : 1;
                return "Returned for corporate clarification · Corporate level " + lvl;
            }
            if (bf == BankFlowState.BANK_NEW || bf == BankFlowState.BANK_IN_REVIEW || bf == BankFlowState.BANK_CLARIFICATION) {
                int bl = bcl != null ? bcl : 1;
                return "With bank for review · Bank level " + bl;
            }
        }
        if (cf == CorporateFlowState.CORP_APPROVED_HOLD) {
            return "Corporate approved (on hold) · Corporate (send to bank)";
        }
        if (cf == CorporateFlowState.CORP_CLARIFICATION
                && st == PayrollBatchStatus.PENDING
                && bf == BankFlowState.BANK_SENT_TO_CORPORATE) {
            int lvl = ccl != null ? ccl : 1;
            return "Corporate clarification after bank return · Corporate level " + lvl;
        }
        if (cf == CorporateFlowState.CORP_NEW || cf == CorporateFlowState.CORP_IN_REVIEW || cf == CorporateFlowState.CORP_CLARIFICATION) {
            if (ccl != null) {
                return "Corporate review · Corporate level " + ccl;
            }
            return "Draft (not submitted) · Corporate";
        }
        return (st != null ? st.name() : "Unknown") + " · —";
    }

    private static String paymentBatchKindLabel(PaymentBatchKind k) {
        if (k == null) {
            return "Payroll payment";
        }
        return switch (k) {
            case VENDOR -> "Vendor payment";
            case PAYROLL -> "Payroll payment";
        };
    }
}
