package com.payprosys.controller;

import com.payprosys.auth.CurrentUser;
import com.payprosys.auth.SessionInfo;
import com.payprosys.dto.ActivateRequest;
import com.payprosys.dto.ApiResponse;
import com.payprosys.dto.LoginRequest;
import com.payprosys.dto.LoginResponse;
import com.payprosys.dto.OrganizationProfileDto;
import com.payprosys.service.AuthService;
import com.payprosys.service.ProfileService;
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
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;
    private final ProfileService profileService;

    @GetMapping("/whoami")
    public ResponseEntity<ApiResponse<?>> whoami(HttpServletRequest request) {
        SessionInfo session = currentUser.getSession(request);
        if (session == null) {
            return ResponseEntity.ok(ApiResponse.success(Map.of("principal", "null", "authorities", Collections.emptyList())));
        }
        OrganizationProfileDto org = profileService.getOrganizationProfile(session);
        String bankLogoUrl = "BANK".equals(org.getOrganizationType()) ? org.getLogoUrl() : null;
        String corporateLogoUrl = "CORPORATE".equals(org.getOrganizationType()) ? org.getLogoUrl() : null;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("principal", session.getEmail());
        payload.put("userId", session.getUserId() != null ? session.getUserId().toString() : null);
        payload.put("authorities", session.getRoles());
        payload.put("bankId", session.getBankId() != null ? session.getBankId().toString() : null);
        payload.put("corporateId", session.getCorporateId() != null ? session.getCorporateId().toString() : null);
        payload.put("bankName", session.getBankName() != null ? session.getBankName() : null);
        payload.put("corporateName", session.getCorporateName() != null ? session.getCorporateName() : null);
        payload.put("bankLogoUrl", bankLogoUrl);
        payload.put("corporateLogoUrl", corporateLogoUrl);
        return ResponseEntity.ok(ApiResponse.success(payload));
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
