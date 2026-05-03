package com.payprosys.controller;

import com.payprosys.auth.CurrentUser;
import com.payprosys.dto.ApiResponse;
import com.payprosys.dto.RoleDto;
import com.payprosys.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final CurrentUser currentUser;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleDto>>> getAllRoles(HttpServletRequest request) {
        currentUser.requireRole(request, "SUPER_ADMIN", "BANK_ADMIN", "BANK_USER", "CORP_ADMIN", "CORP_USER");
        List<RoleDto> roles = roleService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success(roles));
    }
}
