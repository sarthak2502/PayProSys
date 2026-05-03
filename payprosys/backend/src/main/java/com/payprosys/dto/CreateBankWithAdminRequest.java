package com.payprosys.dto;

import jakarta.validation.constraints.NotBlank;
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
public class CreateBankWithAdminRequest {

    @NotBlank(message = "Bank name is required")
    private String name;

    @NotBlank(message = "Bank admin email is required")
    private String adminEmail;

    @NotBlank(message = "Bank admin password is required")
    private String adminPassword;

    @NotBlank(message = "Bank admin first name is required")
    private String adminFirstName;

    @NotBlank(message = "Bank admin last name is required")
    private String adminLastName;
}
