package com.payprosys.service;

import com.payprosys.dto.CorporateDto;
import com.payprosys.dto.CorporateWithAdminDto;
import com.payprosys.dto.CreateCorporateRequest;
import com.payprosys.dto.CreateCorporateResponse;
import com.payprosys.dto.CreateCorporateWithAdminRequest;
import com.payprosys.dto.UserDto;
import com.payprosys.entity.Bank;
import com.payprosys.entity.Corporate;
import com.payprosys.entity.CorporateBankUserAssignment;
import com.payprosys.entity.CorporateStatus;
import com.payprosys.entity.Role;
import com.payprosys.entity.RoleName;
import com.payprosys.entity.User;
import com.payprosys.entity.UserStatus;
import com.payprosys.exception.BadRequestException;
import com.payprosys.exception.ForbiddenException;
import com.payprosys.exception.ResourceNotFoundException;
import com.payprosys.mapper.CorporateMapper;
import com.payprosys.mapper.UserMapper;
import com.payprosys.repository.BankRepository;
import com.payprosys.repository.CorporateBankUserAssignmentRepository;
import com.payprosys.repository.CorporateRepository;
import com.payprosys.repository.RoleRepository;
import com.payprosys.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CorporateServiceImpl implements CorporateService {

    private final CorporateRepository corporateRepository;
    private final BankRepository bankRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CorporateMapper corporateMapper;
    private final CorporateBankUserAssignmentRepository assignmentRepository;
    private final UserMapper userMapper;
    private final WorkflowStepBootstrapService workflowStepBootstrapService;

    @Override
    @Transactional
    public CreateCorporateResponse createCorporateWithAdmin(UUID bankId, CreateCorporateWithAdminRequest request) {
        if (userRepository.existsByEmail(request.getAdminEmail())) {
            throw new BadRequestException("Corporate admin email already in use: " + request.getAdminEmail());
        }
        Bank bank = bankRepository.findById(bankId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank", bankId));
        Corporate corporate = Corporate.builder()
                .name(request.getName())
                .status(CorporateStatus.ACTIVE)
                .bank(bank)
                .build();
        corporate = corporateRepository.save(corporate);
        workflowStepBootstrapService.ensureDefaultCorporateSteps(corporate);
        Role corpAdminRole = roleRepository.findByName(RoleName.CORP_ADMIN)
                .orElseThrow(() -> new IllegalStateException("CORP_ADMIN role not found"));
        User admin = User.builder()
                .firstName(request.getAdminFirstName())
                .lastName(request.getAdminLastName())
                .email(request.getAdminEmail())
                .password(request.getAdminPassword())
                .status(UserStatus.ACTIVE)
                .corporate(corporate)
                .roles(List.of(corpAdminRole))
                .build();
        admin = userRepository.save(admin);
        if (request.getAssignedBankUserIds() != null && !request.getAssignedBankUserIds().isEmpty()) {
            for (UUID uid : request.getAssignedBankUserIds()) {
                User bankUser = userRepository.findById(uid).orElse(null);
                if (bankUser != null && bankUser.getBank() != null && bankUser.getBank().getId().equals(bankId)) {
                    if (!assignmentRepository.existsByCorporateIdAndUserId(corporate.getId(), uid)) {
                        assignmentRepository.save(CorporateBankUserAssignment.builder()
                                .corporateId(corporate.getId())
                                .userId(uid)
                                .corporate(corporate)
                                .user(bankUser)
                                .build());
                    }
                }
            }
        }
        return CreateCorporateResponse.builder()
                .corporate(corporateMapper.toDto(corporate))
                .adminEmail(admin.getEmail())
                .adminPassword(admin.getPassword())
                .adminFirstName(admin.getFirstName())
                .adminLastName(admin.getLastName())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorporateWithAdminDto> getCorporatesByBankWithAdmin(UUID bankId) {
        List<Corporate> corporates = corporateRepository.findByBankIdOrderByCreatedAtDesc(bankId);
        List<CorporateWithAdminDto> result = new ArrayList<>();
        for (Corporate c : corporates) {
            User admin = userRepository.findByCorporateIdWithRoles(c.getId()).stream()
                    .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName() == RoleName.CORP_ADMIN))
                    .findFirst()
                    .orElse(null);
            result.add(CorporateWithAdminDto.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .status(c.getStatus())
                    .createdAt(c.getCreatedAt())
                    .bankId(c.getBank().getId())
                    .adminEmail(admin != null ? admin.getEmail() : null)
                    .adminPassword(admin != null ? admin.getPassword() : null)
                    .adminFirstName(admin != null ? admin.getFirstName() : null)
                    .adminLastName(admin != null ? admin.getLastName() : null)
                    .build());
        }
        return result;
    }

    @Override
    @Transactional
    public CorporateDto createCorporate(CreateCorporateRequest request) {
        Bank bank = bankRepository.findById(request.getBankId())
                .orElseThrow(() -> new ResourceNotFoundException("Bank", request.getBankId()));
        Corporate corporate = Corporate.builder()
                .name(request.getName())
                .status(CorporateStatus.ACTIVE)
                .bank(bank)
                .build();
        corporate = corporateRepository.save(corporate);
        workflowStepBootstrapService.ensureDefaultCorporateSteps(corporate);
        return corporateMapper.toDto(corporate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorporateDto> getCorporatesByBank(UUID bankId) {
        return corporateRepository.findByBankIdOrderByCreatedAtDesc(bankId).stream()
                .map(corporateMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAssignedBankUsers(UUID corporateId, UUID bankId) {
        Corporate corporate = corporateRepository.findById(corporateId)
                .orElseThrow(() -> new ResourceNotFoundException("Corporate", corporateId));
        if (!corporate.getBank().getId().equals(bankId)) {
            throw new ForbiddenException("Corporate does not belong to your bank");
        }
        return assignmentRepository.findByCorporateIdOrderByUserId(corporateId).stream()
                .map(a -> userMapper.toDto(a.getUser()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignBankUser(UUID corporateId, UUID userId, UUID bankId) {
        Corporate corporate = corporateRepository.findById(corporateId)
                .orElseThrow(() -> new ResourceNotFoundException("Corporate", corporateId));
        if (!corporate.getBank().getId().equals(bankId)) {
            throw new ForbiddenException("Corporate does not belong to your bank");
        }
        User bankUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (bankUser.getBank() == null || !bankUser.getBank().getId().equals(bankId)) {
            throw new BadRequestException("User is not a bank user of this bank");
        }
        if (assignmentRepository.existsByCorporateIdAndUserId(corporateId, userId)) {
            return;
        }
        assignmentRepository.save(CorporateBankUserAssignment.builder()
                .corporateId(corporateId)
                .userId(userId)
                .corporate(corporate)
                .user(bankUser)
                .build());
    }

    @Override
    @Transactional
    public void unassignBankUser(UUID corporateId, UUID userId, UUID bankId) {
        Corporate corporate = corporateRepository.findById(corporateId)
                .orElseThrow(() -> new ResourceNotFoundException("Corporate", corporateId));
        if (!corporate.getBank().getId().equals(bankId)) {
            throw new ForbiddenException("Corporate does not belong to your bank");
        }
        assignmentRepository.deleteByCorporateIdAndUserId(corporateId, userId);
    }
}
