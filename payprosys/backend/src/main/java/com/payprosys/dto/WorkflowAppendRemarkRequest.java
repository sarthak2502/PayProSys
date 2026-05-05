package com.payprosys.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowAppendRemarkRequest {

    private String body;

    @Builder.Default
    private boolean bankVisible = false;

    @Builder.Default
    private String eventType = "REMARK";
}
