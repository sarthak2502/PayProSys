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
    /** Present when listing for bank context. */
    private String corporateName;
}
