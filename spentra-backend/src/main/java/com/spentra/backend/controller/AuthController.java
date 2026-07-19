package com.spentra.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spentra.backend.model.dto.auth.GoogleAuthRequest;
import com.spentra.backend.model.dto.auth.LoginRequest;
import com.spentra.backend.model.dto.auth.LoginResponse;
import com.spentra.backend.model.dto.auth.SignupRequest;
import com.spentra.backend.model.dto.auth.SignupResponse;
import com.spentra.backend.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signUp")
    public ResponseEntity<SignupResponse> signUp(@Valid @RequestBody SignupRequest signupRequest) {
        return ResponseEntity.ok(authService.signUp(signupRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/google")
    public ResponseEntity<LoginResponse> googleLogin(@Valid @RequestBody GoogleAuthRequest googleAuthRequest) {
        return ResponseEntity.ok(authService.googleLogin(googleAuthRequest));
    }
}
