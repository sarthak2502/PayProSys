package com.payprosys.dto;

import com.payprosys.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private UserStatus status;
    private Instant createdAt;
    private UUID bankId;
    private UUID corporateId;
    private String bankName;
    private String corporateName;
    private List<String> roles;
    /** Only populated for super admin "all users" list (for testing/login). */
    private String password;
}
