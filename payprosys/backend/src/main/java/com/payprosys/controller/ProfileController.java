package com.payprosys.controller;

import com.payprosys.auth.CurrentUser;
import com.payprosys.auth.SessionInfo;
import com.payprosys.dto.ApiResponse;
import com.payprosys.dto.ChangePasswordRequest;
import com.payprosys.dto.OrganizationProfileDto;
import com.payprosys.dto.UpdateBrandingRequest;
import com.payprosys.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final CurrentUser currentUser;

    @GetMapping("/organization")
    public ResponseEntity<ApiResponse<OrganizationProfileDto>> getOrganization(HttpServletRequest request) {
        SessionInfo session = currentUser.requireSession(request);
        return ResponseEntity.ok(ApiResponse.success(profileService.getOrganizationProfile(session)));
    }

    @PatchMapping("/organization/branding")
    public ResponseEntity<ApiResponse<OrganizationProfileDto>> updateBranding(
            HttpServletRequest request,
            @Valid @RequestBody UpdateBrandingRequest body) {
        SessionInfo session = currentUser.requireSession(request);
        profileService.updateOrganizationLogo(session, body.getLogoUrl());
        return ResponseEntity.ok(ApiResponse.success("Branding updated", profileService.getOrganizationProfile(session)));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            HttpServletRequest request,
            @Valid @RequestBody ChangePasswordRequest body) {
        SessionInfo session = currentUser.requireSession(request);
        profileService.changePassword(session, body.getCurrentPassword(), body.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password updated", null));
    }
}
