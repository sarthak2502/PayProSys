package com.payprosys.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;
    private String email;
    private List<String> roles;
    private java.util.UUID bankId;
    private java.util.UUID corporateId;
    private String bankName;
    private String corporateName;
}
