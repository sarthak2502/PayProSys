package com.payprosys.service;

import com.payprosys.auth.SessionInfo;
import com.payprosys.auth.SessionStore;
import com.payprosys.dto.LoginRequest;
import com.payprosys.dto.LoginResponse;
import com.payprosys.entity.User;
import com.payprosys.exception.BadRequestException;
import com.payprosys.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final SessionStore sessionStore;
    private final UserService userService;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));
        if (user.getStatus() != com.payprosys.entity.UserStatus.ACTIVE) {
            throw new BadRequestException("Account is not active");
        }
        // Demo: plain text password (V5 migration sets admin password to "password")
        if (!user.getPassword().equals(request.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }
        String token = UUID.randomUUID().toString();
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toList());
        UUID bankId = user.getBank() != null ? user.getBank().getId() : null;
        UUID corporateId = user.getCorporate() != null ? user.getCorporate().getId() : null;
        String bankName = user.getBank() != null ? user.getBank().getName() : null;
        String corporateName = user.getCorporate() != null ? user.getCorporate().getName() : null;
        SessionInfo session = SessionInfo.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .roles(roles)
                .bankId(bankId)
                .corporateId(corporateId)
                .bankName(bankName)
                .corporateName(corporateName)
                .build();
        sessionStore.put(token, session);
        return LoginResponse.builder()
                .token(token)
                .email(user.getEmail())
                .roles(roles)
                .bankId(bankId)
                .corporateId(corporateId)
                .bankName(bankName)
                .corporateName(corporateName)
                .build();
    }

    @Override
    @Transactional
    public void activate(String activationToken, String password) {
        userService.activateUser(activationToken, password);
    }
}
