package com.payprosys.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollUploadResponse {

    private UUID batchId;
    private int totalRecords;
    private BigDecimal totalAmount;
    private String fileName;
    private Integer yearMonth;
    private String batchStatus;
    private String paymentBatchKind;
}
