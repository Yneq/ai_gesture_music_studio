package com.vance.gesturemusicstudio.service.impl;

import com.vance.gesturemusicstudio.dao.UserDao;
import com.vance.gesturemusicstudio.dto.auth.RegisterRequest;
import com.vance.gesturemusicstudio.model.User;
import com.vance.gesturemusicstudio.exception.ApiException;
import com.vance.gesturemusicstudio.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        if (userDao.existsByUsername(request.getUsername())) {
            throw new ApiException(HttpStatus.CONFLICT, "使用者名稱已被使用");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()
                && userDao.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email 已被使用");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .build();

        return userDao.save(user);
    }

    @Override
    public User getById(Long id) {
        return userDao.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "找不到使用者"));
    }

    @Override
    public boolean existsByUsername(String username) {
        return userDao.existsByUsername(username);
    }

    @Override
    @Transactional
    public User findOrCreateGoogleUser(String googleId, String email, String name, String avatarUrl) {
        // 1. Already linked Google account
        var byGoogle = userDao.findByGoogleId(googleId);
        if (byGoogle.isPresent()) {
            User user = byGoogle.get();
            if (avatarUrl != null && !avatarUrl.equals(user.getAvatarUrl())) {
                user.setAvatarUrl(avatarUrl);
                return userDao.save(user);
            }
            return user;
        }

        // 2. Existing email/password account — link Google to it
        if (email != null && !email.isBlank()) {
            var byEmail = userDao.findByEmail(email);
            if (byEmail.isPresent()) {
                User user = byEmail.get();
                user.setGoogleId(googleId);
                user.setAvatarUrl(avatarUrl);
                return userDao.save(user);
            }
        }

        // 3. Create new user from Google profile
        String base = deriveName(name, email);
        String username = uniqueUsername(base);

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .email(email)
                .googleId(googleId)
                .avatarUrl(avatarUrl)
                .build();

        return userDao.save(user);
    }

    private String deriveName(String name, String email) {
        if (name != null && !name.isBlank()) {
            String cleaned = name.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
            if (!cleaned.isBlank()) return cleaned.substring(0, Math.min(cleaned.length(), 30));
        }
        if (email != null && email.contains("@")) {
            return email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
        }
        return "user";
    }

    private String uniqueUsername(String base) {
        if (base.isBlank()) base = "user";
        String candidate = base;
        int i = 2;
        while (userDao.existsByUsername(candidate)) {
            candidate = base + i++;
        }
        return candidate;
    }
}
