package com.payprosys.entity;

/** Pending = uploaded, not finalized. Submitted = visible to banks and employee payment rollups. */
public enum PayrollBatchStatus {
    PENDING,
    SUBMITTED
}
