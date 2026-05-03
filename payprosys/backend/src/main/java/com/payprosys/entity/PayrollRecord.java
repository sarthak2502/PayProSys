package com.payprosys.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payroll_records", indexes = @Index(name = "idx_payroll_record_batch_id", columnList = "payroll_batch_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_batch_id", nullable = false, updatable = false)
    private PayrollBatch payrollBatch;

    @Column(name = "employee_name", nullable = false)
    private String employeeName;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Column(name = "cpr_id", nullable = false)
    private String cprId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** Payment details (e.g. salary, reimbursement). */
    @Column(name = "payment_for", length = 500)
    private String paymentFor;
}
