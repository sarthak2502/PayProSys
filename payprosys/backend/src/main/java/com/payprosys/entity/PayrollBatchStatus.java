package com.payprosys.entity;

/** Pending = uploaded, not finalized. Submitted = with bank. Completed = payment processed (terminal). */
public enum PayrollBatchStatus {
    PENDING,
    SUBMITTED,
    COMPLETED
}
