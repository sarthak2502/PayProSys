package com.payprosys.mapper;

import com.payprosys.dto.PayrollBatchDto;
import com.payprosys.entity.PayrollBatch;
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
                .corporateName(entity.getCorporate() != null ? entity.getCorporate().getName() : null)
                .build();
    }
}
