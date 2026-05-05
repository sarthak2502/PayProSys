package com.payprosys.service;

import com.payprosys.auth.SessionInfo;
import com.payprosys.dto.OrganizationProfileDto;
import com.payprosys.entity.Bank;
import com.payprosys.entity.Corporate;
import com.payprosys.entity.User;
import com.payprosys.exception.BadRequestException;
import com.payprosys.exception.ForbiddenException;
import com.payprosys.repository.BankRepository;
import com.payprosys.repository.CorporateRepository;
import com.payprosys.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final BankRepository bankRepository;
    private final CorporateRepository corporateRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public OrganizationProfileDto getOrganizationProfile(SessionInfo session) {
        if (session.getCorporateId() != null) {
            return corporateRepository.findById(session.getCorporateId())
                    .map(c -> OrganizationProfileDto.builder()
                            .organizationType("CORPORATE")
                            .organizationId(c.getId())
                            .organizationName(c.getName())
                            .logoUrl(c.getLogoUrl())
                            .build())
                    .orElseGet(() -> OrganizationProfileDto.builder()
                            .organizationType("CORPORATE")
                            .organizationId(session.getCorporateId())
                            .organizationName(session.getCorporateName())
                            .logoUrl(null)
                            .build());
        }
        if (session.getBankId() != null) {
            return bankRepository.findById(session.getBankId())
                    .map(b -> OrganizationProfileDto.builder()
                            .organizationType("BANK")
                            .organizationId(b.getId())
                            .organizationName(b.getName())
                            .logoUrl(b.getLogoUrl())
                            .build())
                    .orElseGet(() -> OrganizationProfileDto.builder()
                            .organizationType("BANK")
                            .organizationId(session.getBankId())
                            .organizationName(session.getBankName())
                            .logoUrl(null)
                            .build());
        }
        return OrganizationProfileDto.builder()
                .organizationType(null)
                .organizationId(null)
                .organizationName(null)
                .logoUrl(null)
                .build();
    }

    @Override
    @Transactional
    public void updateOrganizationLogo(SessionInfo session, String logoUrl) {
        String normalized = normalizeLogoUrl(logoUrl);
        if (session.getCorporateId() != null) {
            requireAnyRole(session, "CORP_ADMIN", "CORP_USER");
            Corporate c = corporateRepository.findById(session.getCorporateId())
                    .orElseThrow(() -> new BadRequestException("Corporate not found"));
            c.setLogoUrl(normalized);
            corporateRepository.save(c);
            return;
        }
        if (session.getBankId() != null) {
            requireAnyRole(session, "BANK_ADMIN", "BANK_USER");
            Bank b = bankRepository.findById(session.getBankId())
                    .orElseThrow(() -> new BadRequestException("Bank not found"));
            b.setLogoUrl(normalized);
            bankRepository.save(b);
            return;
        }
        throw new BadRequestException("No bank or corporate is linked to your account");
    }

    @Override
    @Transactional
    public void changePassword(SessionInfo session, String currentPassword, String newPassword) {
        UUID userId = session.getUserId();
        if (userId == null) {
            throw new ForbiddenException("Not authenticated");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        String cur = currentPassword != null ? currentPassword.trim() : "";
        String nw = newPassword != null ? newPassword.trim() : "";
        if (!user.getPassword().equals(cur)) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (nw.length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters");
        }
        user.setPassword(nw);
        userRepository.save(user);
    }

    private static void requireAnyRole(SessionInfo session, String... roles) {
        List<String> userRoles = session.getRoles();
        for (String r : roles) {
            if (userRoles.contains(r)) {
                return;
            }
        }
        throw new ForbiddenException("Insufficient role to update organization branding");
    }

    private static String normalizeLogoUrl(String logoUrl) {
        if (logoUrl == null) {
            return null;
        }
        String t = logoUrl.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.length() > 1024) {
            throw new BadRequestException("Logo URL is too long");
        }
        if (t.startsWith("data:image/")) {
            return t;
        }
        if (t.startsWith("https://") || t.startsWith("http://")) {
            return t;
        }
        throw new BadRequestException("Logo must be an http(s) URL or a small data:image/… URL");
    }
}
