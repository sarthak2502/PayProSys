package com.payprosys.service;

import com.payprosys.dto.PayrollBatchDto;
import com.payprosys.dto.PayrollRecordDto;
import com.payprosys.dto.PayrollUploadResponse;
import com.payprosys.entity.PaymentBatchKind;
import com.payprosys.entity.PayrollBatchStatus;

import java.util.List;
import java.util.UUID;

public interface PayrollService {

    PayrollUploadResponse uploadPayrollExcel(
            UUID corporateId, UUID uploadedById, String fileName, byte[] content, Integer yearMonth, PaymentBatchKind paymentBatchKind);

    /** Single batch if the actor may view it (corporate member, or bank when batch is visible to bank). */
    PayrollBatchDto getBatchById(UUID batchId, UUID actorCorporateId, UUID actorBankId, UUID actorUserId, List<String> actorRoles);

    /** If status is null, return all batches for the corporate. */
    List<PayrollBatchDto> getBatchesByCorporate(UUID corporateId, PayrollBatchStatus status, PaymentBatchKind paymentBatchKind);

    /** Submitted batches for all corporates under the bank. */
    List<PayrollBatchDto> getSubmittedBatchesForBank(UUID bankId);

    /** Completed (payment processed) batches for all corporates under the bank. */
    List<PayrollBatchDto> getCompletedBatchesForBank(UUID bankId);

    /** Only lines from submitted batches. */
    List<PayrollRecordDto> getRecordsByCorporate(UUID corporateId, Integer yearMonth);

    /** Records in a batch; caller must be allowed (corporate owns batch, or bank owns corporate and batch is SUBMITTED). */
    List<PayrollRecordDto> getRecordsByBatchId(UUID batchId, UUID actorCorporateId, UUID actorBankId);

    void submitBatch(UUID batchId, UUID corporateId, UUID actorUserId);

    void deleteBatch(UUID batchId, UUID corporateId);

    /** Ensures the corporate belongs to the bank or throws Forbidden. */
    void assertCorporateUnderBank(UUID corporateId, UUID bankId);
}
