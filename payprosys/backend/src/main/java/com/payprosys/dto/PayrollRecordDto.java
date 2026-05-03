package com.payprosys.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollRecordDto {

    private UUID id;
    private UUID payrollBatchId;
    /** Year-month of the batch (YYYYMM). */
    private Integer yearMonth;
    private Instant batchCreatedAt;
    private String fileName;

    private String employeeName;
    private String accountNumber;
    private LocalDate joiningDate;
    private String cprId;
    private BigDecimal amount;
    private String paymentFor;
}
