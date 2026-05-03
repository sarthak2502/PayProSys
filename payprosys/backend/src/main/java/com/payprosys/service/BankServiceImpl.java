package com.payprosys.service;

import com.payprosys.dto.BankDto;
import com.payprosys.dto.BankWithAdminDto;
import com.payprosys.dto.CreateBankRequest;
import com.payprosys.dto.CreateBankResponse;
import com.payprosys.dto.CreateBankWithAdminRequest;
import com.payprosys.entity.Bank;
import com.payprosys.entity.Role;
import com.payprosys.entity.RoleName;
import com.payprosys.entity.User;
import com.payprosys.entity.UserStatus;
import com.payprosys.exception.BadRequestException;
import com.payprosys.mapper.BankMapper;
import com.payprosys.repository.BankRepository;
import com.payprosys.repository.RoleRepository;
import com.payprosys.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BankServiceImpl implements BankService {

    private final BankRepository bankRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BankMapper bankMapper;
    private final WorkflowStepBootstrapService workflowStepBootstrapService;

    @Override
    @Transactional
    public CreateBankResponse createBankWithAdmin(CreateBankWithAdminRequest request) {
        if (userRepository.existsByEmail(request.getAdminEmail())) {
            throw new BadRequestException("Bank admin email already in use: " + request.getAdminEmail());
        }
        Bank bank = Bank.builder()
                .name(request.getName())
                .build();
        bank = bankRepository.save(bank);
        workflowStepBootstrapService.ensureDefaultBankSteps(bank);
        Role bankAdminRole = roleRepository.findByName(RoleName.BANK_ADMIN)
                .orElseThrow(() -> new IllegalStateException("BANK_ADMIN role not found"));
        User admin = User.builder()
                .firstName(request.getAdminFirstName())
                .lastName(request.getAdminLastName())
                .email(request.getAdminEmail())
                .password(request.getAdminPassword())
                .status(UserStatus.ACTIVE)
                .bank(bank)
                .roles(List.of(bankAdminRole))
                .build();
        admin = userRepository.save(admin);
        return CreateBankResponse.builder()
                .bank(bankMapper.toDto(bank))
                .adminEmail(admin.getEmail())
                .adminPassword(admin.getPassword())
                .adminFirstName(admin.getFirstName())
                .adminLastName(admin.getLastName())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankWithAdminDto> getAllBanksWithAdmin() {
        List<Bank> banks = bankRepository.findAll();
        List<BankWithAdminDto> result = new ArrayList<>();
        for (Bank bank : banks) {
            User admin = userRepository.findByBankIdWithRoles(bank.getId()).stream()
                    .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName() == RoleName.BANK_ADMIN))
                    .findFirst()
                    .orElse(null);
            result.add(BankWithAdminDto.builder()
                    .id(bank.getId())
                    .name(bank.getName())
                    .createdAt(bank.getCreatedAt())
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
    public BankDto createBank(CreateBankRequest request) {
        Bank bank = Bank.builder()
                .name(request.getName())
                .build();
        bank = bankRepository.save(bank);
        workflowStepBootstrapService.ensureDefaultBankSteps(bank);
        return bankMapper.toDto(bank);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankDto> getAllBanks() {
        return bankRepository.findAll().stream()
                .map(bankMapper::toDto)
                .collect(Collectors.toList());
    }
}
