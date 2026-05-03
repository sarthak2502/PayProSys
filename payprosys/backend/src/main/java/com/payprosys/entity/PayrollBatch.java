package com.payprosys.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "payroll_batches", indexes = @Index(name = "idx_payroll_batch_corporate_id", columnList = "corporate_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corporate_id", nullable = false, updatable = false)
    private Corporate corporate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", nullable = false, updatable = false)
    private User uploadedBy;

    @Column(name = "total_records", nullable = false)
    private int totalRecords;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "file_name")
    private String fileName;

    /** Year-month for this payroll (YYYYMM, e.g. 202503). Set when corporate admin selects month on upload. */
    @Column(name = "year_month")
    private Integer yearMonth;

    /** PENDING until corporate submits; then banks can see batch. */
    @Enumerated(EnumType.STRING)
    @Column(name = "batch_status", nullable = false)
    @Builder.Default
    private PayrollBatchStatus batchStatus = PayrollBatchStatus.SUBMITTED;

    /** Richer corporate lifecycle (Phase 1); see CorporateFlowState. */
    @Enumerated(EnumType.STRING)
    @Column(name = "corporate_flow_state", nullable = false, length = 40)
    @Builder.Default
    private CorporateFlowState corporateFlowState = CorporateFlowState.CORP_NEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "bank_flow_state", length = 40)
    private BankFlowState bankFlowState;

    /** Payroll vs vendor payout; set at upload. */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_batch_kind", nullable = false, length = 20)
    @Builder.Default
    private PaymentBatchKind paymentBatchKind = PaymentBatchKind.PAYROLL;

    /** 1 = L1, 2 = L2, … while batch is with corporate reviewers. */
    @Column(name = "current_corporate_review_level")
    private Integer currentCorporateReviewLevel;

    @Column(name = "current_bank_review_level")
    private Integer currentBankReviewLevel;

    /** Only this text (with send-to-bank) is intended to be visible to the bank; internal remarks use review_events. */
    @Column(name = "remarks_for_bank", columnDefinition = "TEXT")
    private String remarksForBank;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "payrollBatch", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<PayrollRecord> payrollRecords = new ArrayList<>();

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
