package com.payprosys.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCorporateRequest {

    @NotNull(message = "Bank ID is required")
    private UUID bankId;

    @jakarta.validation.constraints.NotBlank(message = "Corporate name is required")
    private String name;
}
