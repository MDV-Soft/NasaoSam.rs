package org.example.nasaosam.auth.controller;

import jakarta.validation.Valid;
import org.example.nasaosam.auth.dto.LoginRequest;
import org.example.nasaosam.auth.dto.RegisterRequest;
import org.example.nasaosam.auth.dto.AuthResponse;
import org.example.nasaosam.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response;

        response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> result;

        result = new LinkedHashMap<>();
        result.put("userId", jwt.getSubject());
        result.put("email", jwt.getClaimAsString("email"));
        result.put("role", jwt.getClaimAsString("role"));
        return result;
    }
}
