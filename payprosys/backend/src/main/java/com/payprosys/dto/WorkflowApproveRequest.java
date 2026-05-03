package com.payprosys.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Corporate approve at current review level; optional combined send-to-bank on final level (POC §6). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowApproveRequest {

    private String remarks;

    @Builder.Default
    private boolean sendToBankAfter = false;

    /** Required when sendToBankAfter is true; stored as remarks_for_bank and bank-visible audit. */
    private String remarksForBank;
}
