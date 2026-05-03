package com.payprosys.controller;

import com.payprosys.auth.CurrentUser;
import com.payprosys.auth.SessionInfo;
import com.payprosys.dto.ActivateRequest;
import com.payprosys.dto.ApiResponse;
import com.payprosys.dto.LoginRequest;
import com.payprosys.dto.LoginResponse;
import com.payprosys.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;

    @GetMapping("/whoami")
    public ResponseEntity<ApiResponse<?>> whoami(HttpServletRequest request) {
        SessionInfo session = currentUser.getSession(request);
        if (session == null) {
            return ResponseEntity.ok(ApiResponse.success(Map.of("principal", "null", "authorities", Collections.emptyList())));
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "principal", session.getEmail(),
                "authorities", session.getRoles(),
                "bankId", session.getBankId() != null ? session.getBankId().toString() : null,
                "corporateId", session.getCorporateId() != null ? session.getCorporateId().toString() : null,
                "bankName", session.getBankName() != null ? session.getBankName() : null,
                "corporateName", session.getCorporateName() != null ? session.getCorporateName() : null)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<Void>> activate(@Valid @RequestBody ActivateRequest request) {
        authService.activate(request.getActivationToken(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.success("Account activated successfully", null));
    }
}
