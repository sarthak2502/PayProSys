package com.payprosys.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Response after creating a corporate: corporate details + admin login for handing to corporate admin. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCorporateResponse {
    private CorporateDto corporate;
    private String adminEmail;
    private String adminPassword;
    private String adminFirstName;
    private String adminLastName;
}
