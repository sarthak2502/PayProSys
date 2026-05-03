package com.payprosys.entity;

/** Bank-side batch lifecycle after corporate send-to-bank. */
public enum BankFlowState {
    BANK_NEW,
    BANK_IN_REVIEW,
    BANK_SENT_TO_CORPORATE,
    BANK_CLARIFICATION,
    BANK_APPROVED,
    BANK_PROCESS_PAYMENT
}
