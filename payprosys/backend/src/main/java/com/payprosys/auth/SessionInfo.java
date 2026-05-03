package com.payprosys.auth;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/** Demo: in-memory session after login. */
@Value
@Builder
public class SessionInfo {
    String token;
    UUID userId;
    String email;
    List<String> roles;
    UUID bankId;
    UUID corporateId;
    String bankName;      // for header display
    String corporateName; // for header display
}
