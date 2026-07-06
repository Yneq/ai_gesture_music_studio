package com.vance.gesturemusicstudio.service.impl;

import com.vance.gesturemusicstudio.dto.auth.AuthResponse;
import com.vance.gesturemusicstudio.dto.auth.LoginRequest;
import com.vance.gesturemusicstudio.dto.auth.RegisterRequest;
import com.vance.gesturemusicstudio.dto.auth.UserResponse;
import com.vance.gesturemusicstudio.exception.ApiException;
import com.vance.gesturemusicstudio.model.User;
import com.vance.gesturemusicstudio.service.AuthService;
import com.vance.gesturemusicstudio.service.UserService;
import com.vance.gesturemusicstudio.util.CustomUserDetails;
import com.vance.gesturemusicstudio.util.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RestTemplate restTemplate;

    @Value("${app.google.client-id:}")
    private String googleClientId;

    @Override
    public AuthResponse register(RegisterRequest request) {
        var user = userService.register(request);
        var userDetails = new CustomUserDetails(user);
        return AuthResponse.builder()
                .token(jwtService.generateToken(userDetails))
                .user(UserResponse.from(user))
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        if (!userService.existsByUsername(request.getUsername())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "帳號不存在");
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            return AuthResponse.builder()
                    .token(jwtService.generateToken(userDetails))
                    .user(UserResponse.from(userDetails.getUser()))
                    .build();
        } catch (BadCredentialsException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "密碼錯誤");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public AuthResponse loginWithGoogle(String credential) {
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(
                    "https://oauth2.googleapis.com/tokeninfo?id_token=" + credential,
                    Map.class
            );
            Map<String, Object> payload = resp.getBody();
            if (payload == null || resp.getStatusCode().isError()) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Google 憑證驗證失敗");
            }

            // Verify token audience if client-id is configured
            if (googleClientId != null && !googleClientId.isBlank()) {
                String aud = (String) payload.get("aud");
                if (!googleClientId.equals(aud)) {
                    throw new ApiException(HttpStatus.UNAUTHORIZED, "Google Client ID 不符");
                }
            }

            if (!"true".equals(String.valueOf(payload.get("email_verified")))) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Google Email 未驗證");
            }

            String googleId = (String) payload.get("sub");
            String email    = (String) payload.get("email");
            String name     = (String) payload.get("name");
            String picture  = (String) payload.get("picture");

            User user = userService.findOrCreateGoogleUser(googleId, email, name, picture);
            var userDetails = new CustomUserDetails(user);
            return AuthResponse.builder()
                    .token(jwtService.generateToken(userDetails))
                    .user(UserResponse.from(user))
                    .build();

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Google 登入失敗：" + e.getMessage());
        }
    }
}
