package com.payprosys.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollBatchDto {

    private UUID id;
    private UUID corporateId;
    private UUID uploadedById;
    private int totalRecords;
    private BigDecimal totalAmount;
    private String fileName;
    /** Year-month for this batch (YYYYMM). */
    private Integer yearMonth;
    private Instant createdAt;
    private String batchStatus;
    /** Human-readable status for UI (draft vs corporate review vs sent to bank). */
    private String batchStatusLabel;
    /** One-line English status and responsible party for inbox/history tables. */
    private String workflowListSummary;
    /** True when this batch appears in corporate inbox for read-only (past reviewer) access. */
    private Boolean viewOnly;
    /** Corporate workflow state (Phase 1). */
    private String corporateFlowState;
    /** Bank workflow state once batch is with the bank; null until then. */
    private String bankFlowState;
    private Integer currentCorporateReviewLevel;
    private Integer currentBankReviewLevel;
    private String remarksForBank;
    /** Present when listing for bank context. */
    private String corporateName;
    /** PAYROLL or VENDOR from upload. */
    private String paymentBatchKind;
    /** Human label for UI. */
    private String paymentBatchKindLabel;
}
