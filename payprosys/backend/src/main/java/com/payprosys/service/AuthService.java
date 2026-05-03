package com.payprosys.service;

import com.payprosys.dto.LoginRequest;
import com.payprosys.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void activate(String activationToken, String password);
}
