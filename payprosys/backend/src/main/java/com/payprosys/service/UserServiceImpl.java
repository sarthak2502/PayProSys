package com.payprosys.service;

import com.payprosys.dto.CreateUserRequest;
import com.payprosys.dto.UserDto;
import com.payprosys.entity.Corporate;
import com.payprosys.entity.Role;
import com.payprosys.entity.RoleName;
import com.payprosys.entity.User;
import com.payprosys.entity.UserStatus;
import com.payprosys.exception.BadRequestException;
import com.payprosys.exception.ResourceNotFoundException;
import com.payprosys.mapper.UserMapper;
import com.payprosys.repository.BankRepository;
import com.payprosys.repository.CorporateRepository;
import com.payprosys.repository.RoleRepository;
import com.payprosys.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BankRepository bankRepository;
    private final CorporateRepository corporateRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDto createBankUser(CreateUserRequest request) {
        if (request.getBankId() == null) {
            throw new BadRequestException("Bank ID is required for bank user");
        }
        return createUser(request, request.getBankId(), null, RoleName.BANK_ADMIN, RoleName.BANK_USER);
    }

    @Override
    @Transactional
    public UserDto createCorporateAdmin(CreateUserRequest request) {
        if (request.getCorporateId() == null) {
            throw new BadRequestException("Corporate ID is required for corporate admin");
        }
        return createUser(request, null, request.getCorporateId(), RoleName.CORP_ADMIN);
    }

    @Override
    @Transactional
    public UserDto createCorporateUser(CreateUserRequest request) {
        if (request.getCorporateId() == null) {
            throw new BadRequestException("Corporate ID is required for corporate user");
        }
        return createUser(request, null, request.getCorporateId(), RoleName.CORP_USER);
    }

    private UserDto createUser(CreateUserRequest request, UUID bankId, UUID corporateId, RoleName... allowedRoles) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("User with email already exists: " + request.getEmail());
        }
        List<Role> roles = resolveRoles(request.getRoleIds(), allowedRoles);
        // New users are active by default and can login immediately
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(request.getPassword())
                .status(UserStatus.ACTIVE)
                .activationToken(null)
                .bank(bankId != null ? bankRepository.getReferenceById(bankId) : null)
                .corporate(corporateId != null ? corporateRepository.getReferenceById(corporateId) : null)
                .roles(roles)
                .build();
        user = userRepository.save(user);
        return userMapper.toDto(user);
    }

    private List<Role> resolveRoles(List<Long> roleIds, RoleName[] allowedRoles) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BadRequestException("At least one role is required");
        }
        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new BadRequestException("One or more role IDs are invalid");
        }
        for (Role r : roles) {
            boolean allowed = false;
            for (RoleName name : allowedRoles) {
                if (r.getName() == name) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                throw new BadRequestException("Role " + r.getName() + " is not allowed for this user type");
            }
        }
        return roles;
    }

    @Override
    @Transactional
    public UserDto activateUser(String activationToken, String password) {
        User user = userRepository.findByActivationToken(activationToken)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired activation token"));
        user.setPassword(password); // Demo: plain text
        user.setStatus(UserStatus.ACTIVE);
        user.setActivationToken(null);
        user = userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsersByBank(UUID bankId) {
        return userRepository.findByBankId(bankId).stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsersByCorporate(UUID corporateId) {
        return userRepository.findByCorporateId(corporateId).stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAllWithRolesBankCorporate().stream()
                .map(userMapper::toDtoWithPassword)
                .collect(Collectors.toList());
    }
}
