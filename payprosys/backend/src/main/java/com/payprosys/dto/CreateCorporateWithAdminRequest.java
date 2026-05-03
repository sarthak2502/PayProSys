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
public class CreateCorporateWithAdminRequest {

    @NotBlank(message = "Corporate name is required")
    private String name;

    @NotBlank(message = "Corporate admin email is required")
    private String adminEmail;

    @NotBlank(message = "Corporate admin password is required")
    private String adminPassword;

    @NotBlank(message = "Corporate admin first name is required")
    private String adminFirstName;

    @NotBlank(message = "Corporate admin last name is required")
    private String adminLastName;

    /** Optional: bank user IDs (of this bank) to assign to the new corporate. */
    private java.util.List<java.util.UUID> assignedBankUserIds;
}
