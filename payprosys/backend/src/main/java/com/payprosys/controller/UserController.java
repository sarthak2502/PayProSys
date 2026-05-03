package com.payprosys.controller;

import com.payprosys.auth.CurrentUser;
import com.payprosys.dto.ApiResponse;
import com.payprosys.exception.ForbiddenException;
import com.payprosys.dto.CreateUserRequest;
import com.payprosys.dto.UserDto;
import com.payprosys.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUser currentUser;

    @PostMapping("/bank")
    public ResponseEntity<ApiResponse<UserDto>> createBankUser(HttpServletRequest request, @Valid @RequestBody CreateUserRequest body) {
        currentUser.requireRole(request, "BANK_ADMIN");
        UserDto created = userService.createBankUser(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Bank user created", created));
    }

    @PostMapping("/corporate-admin")
    public ResponseEntity<ApiResponse<UserDto>> createCorporateAdmin(HttpServletRequest request, @Valid @RequestBody CreateUserRequest body) {
        currentUser.requireRole(request, "BANK_ADMIN", "CORP_ADMIN");
        UserDto created = userService.createCorporateAdmin(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Corporate admin created", created));
    }

    @PostMapping("/corporate-user")
    public ResponseEntity<ApiResponse<UserDto>> createCorporateUser(HttpServletRequest request, @Valid @RequestBody CreateUserRequest body) {
        currentUser.requireRole(request, "BANK_ADMIN", "CORP_ADMIN");
        UserDto created = userService.createCorporateUser(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Corporate user created", created));
    }

    @GetMapping("/by-bank")
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsersByBank(HttpServletRequest request, @RequestParam UUID bankId) {
        var session = currentUser.requireRole(request, "BANK_ADMIN", "BANK_USER");
        if (session.getBankId() != null && !session.getBankId().equals(bankId)) {
            throw new ForbiddenException("Cannot list users of another bank");
        }
        List<UserDto> users = userService.getUsersByBank(bankId);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/by-corporate")
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsersByCorporate(HttpServletRequest request, @RequestParam UUID corporateId) {
        var session = currentUser.requireRole(request, "BANK_ADMIN", "BANK_USER", "CORP_ADMIN", "CORP_USER");
        if (session.getCorporateId() != null && !session.getCorporateId().equals(corporateId)) {
            throw new ForbiddenException("Cannot list users of another corporate");
        }
        List<UserDto> users = userService.getUsersByCorporate(corporateId);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /** Super admin only: list all users (bank, corporate, super admin). */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers(HttpServletRequest request) {
        currentUser.requireRole(request, "SUPER_ADMIN");
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }
}
