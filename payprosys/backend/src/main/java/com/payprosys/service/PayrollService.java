package com.payprosys.service;

import com.payprosys.dto.PayrollBatchDto;
import com.payprosys.dto.PayrollRecordDto;
import com.payprosys.dto.PayrollUploadResponse;
import com.payprosys.entity.PayrollBatchStatus;

import java.util.List;
import java.util.UUID;

public interface PayrollService {

    PayrollUploadResponse uploadPayrollExcel(UUID corporateId, UUID uploadedById, String fileName, byte[] content, Integer yearMonth);

    /** If status is null, return all batches for the corporate. */
    List<PayrollBatchDto> getBatchesByCorporate(UUID corporateId, PayrollBatchStatus status);

    /** Submitted batches for all corporates under the bank. */
    List<PayrollBatchDto> getSubmittedBatchesForBank(UUID bankId);

    /** Only lines from submitted batches. */
    List<PayrollRecordDto> getRecordsByCorporate(UUID corporateId, Integer yearMonth);

    /** Records in a batch; caller must be allowed (corporate owns batch, or bank owns corporate and batch is SUBMITTED). */
    List<PayrollRecordDto> getRecordsByBatchId(UUID batchId, UUID actorCorporateId, UUID actorBankId);

    void submitBatch(UUID batchId, UUID corporateId);

    void deleteBatch(UUID batchId, UUID corporateId);

    /** Ensures the corporate belongs to the bank or throws Forbidden. */
    void assertCorporateUnderBank(UUID corporateId, UUID bankId);
}
