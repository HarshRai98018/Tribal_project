package com.klu.service;

import com.klu.dto.*;
import com.klu.exception.ApiException;
import com.klu.model.User;
import com.klu.repository.UserRepository;
import com.klu.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtTokenProvider jwtProvider;
    private final CaptchaService captchaService;

    private static final Set<String> ALLOWED_ROLES = Set.of("admin", "artisan", "customer", "consultant");

    public AuthService(UserRepository userRepo, PasswordEncoder encoder,
                       JwtTokenProvider jwtProvider, CaptchaService captchaService) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwtProvider = jwtProvider;
        this.captchaService = captchaService;
    }

    public AuthResponse login(LoginRequest req) {
        if (!captchaService.verify(req.getCaptchaToken())) {
            throw ApiException.badRequest("Captcha verification failed");
        }

        String email = req.getEmail().trim().toLowerCase();
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));

        if (!encoder.matches(req.getPassword(), user.getPassword())) {
            throw ApiException.unauthorized("Invalid email or password");
        }

        String token = jwtProvider.generateToken(user);
        return new AuthResponse(token, sanitize(user));
    }

    public AuthResponse signup(SignupRequest req) {
        if (!captchaService.verify(req.getCaptchaToken())) {
            throw ApiException.badRequest("Captcha verification failed");
        }

        if (!ALLOWED_ROLES.contains(req.getRole())) {
            throw ApiException.badRequest("Invalid role selected");
        }

        String email = req.getEmail().trim().toLowerCase();
        if (userRepo.existsByEmail(email)) {
            throw ApiException.conflict("Email already registered");
        }

        User user = User.builder()
                .id("U" + System.currentTimeMillis() % 1000000)
                .name(req.getName().trim())
                .email(email)
                .password(encoder.encode(req.getPassword()))
                .role(req.getRole())
                .build();

        userRepo.save(user);
        String token = jwtProvider.generateToken(user);
        return new AuthResponse(token, sanitize(user));
    }

    public Map<String, Object> sanitize(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("email", user.getEmail());
        map.put("role", user.getRole());
        map.put("savedAddress", user.getSavedAddress());
        return map;
    }
}
