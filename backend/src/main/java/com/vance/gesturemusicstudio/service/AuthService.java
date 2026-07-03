package com.vance.gesturemusicstudio.service;

import com.vance.gesturemusicstudio.dto.auth.AuthResponse;
import com.vance.gesturemusicstudio.dto.auth.LoginRequest;
import com.vance.gesturemusicstudio.dto.auth.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse loginWithGoogle(String credential);
}
