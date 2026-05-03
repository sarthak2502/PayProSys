package com.payprosys.service;

import com.payprosys.dto.CreateUserRequest;
import com.payprosys.dto.UserDto;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserDto createBankUser(CreateUserRequest request);

    UserDto createCorporateAdmin(CreateUserRequest request);

    UserDto createCorporateUser(CreateUserRequest request);

    UserDto activateUser(String activationToken, String password);

    List<UserDto> getUsersByBank(UUID bankId);

    List<UserDto> getUsersByCorporate(UUID corporateId);

    List<UserDto> getAllUsers();
}
