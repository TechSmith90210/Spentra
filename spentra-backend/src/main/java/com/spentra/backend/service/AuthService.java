package com.spentra.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.spentra.backend.exception.ApiRequestException;
import com.spentra.backend.model.dto.auth.LoginRequest;
import com.spentra.backend.model.dto.auth.LoginResponse;
import com.spentra.backend.model.dto.auth.SignupRequest;
import com.spentra.backend.model.dto.auth.SignupResponse;
import com.spentra.backend.model.dto.auth.GoogleAuthRequest;
import com.spentra.backend.model.entity.User;
import com.spentra.backend.repository.UserRepository;
import com.spentra.backend.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.Collections;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${GOOGLE_CLIENT_ID}")
    private String googleClientId;

    // signup
    public SignupResponse signUp(SignupRequest signupRequest) {

        if (!signupRequest.getPassword().equals(signupRequest.getConfirmPassword())) {
            throw new ApiRequestException("Passwords do not match!", HttpStatus.BAD_REQUEST);
        }

        // find if user already exists or not
        boolean isUserExists = repository.existsByEmail(signupRequest.getEmail());

        if (isUserExists) {
            throw new ApiRequestException("User Already Exists", HttpStatus.CONFLICT);
        }

        User userToSave = new User();
        userToSave.setName(signupRequest.getName());
        userToSave.setEmail(signupRequest.getEmail());
        userToSave.setPassword(passwordEncoder.encode(signupRequest.getPassword()));

        User savedUser = repository.save(userToSave);

        String token = jwtService.generateToken(savedUser.getId().toString(), signupRequest.getEmail());

        return new SignupResponse(token, savedUser.getName(), savedUser.getEmail(), savedUser.getProfilePic());
    }

    // login
    public LoginResponse login(LoginRequest loginRequest) {

        boolean isUserExists = repository.existsByEmail(loginRequest.getEmail());

        if (isUserExists) {
            User fetchedUser = repository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new ApiRequestException("User not found", HttpStatus.NOT_FOUND));
            // password validation
            boolean isPasswordValid = passwordEncoder.matches(loginRequest.getPassword(),
                    fetchedUser.getPassword());

            if (isPasswordValid) {

                String token = jwtService.generateToken(fetchedUser.getId().toString(), fetchedUser.getEmail());

                return new LoginResponse(
                        token,
                        fetchedUser.getEmail(),
                        fetchedUser.getName(),
                        fetchedUser.getProfilePic());
            }
        } else {
            throw new ApiRequestException("User not found", HttpStatus.NOT_FOUND);
        }

        throw new ApiRequestException("Login failed", HttpStatus.UNAUTHORIZED);
    }

    // google login
    public LoginResponse googleLogin(GoogleAuthRequest request) {
        try {
            NetHttpTransport transport = new NetHttpTransport();
            GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();

                String email = payload.getEmail();
                String name = (String) payload.get("name");
                String pictureUrl = (String) payload.get("picture");

                User user = repository.findByEmail(email).orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setName(name);
                    newUser.setProvider(com.spentra.backend.model.enums.AuthProvider.GOOGLE);
                    newUser.setProfilePic(pictureUrl);
                    return repository.save(newUser);
                });

                String token = jwtService.generateToken(user.getId().toString(), user.getEmail());

                return new LoginResponse(
                        token,
                        user.getEmail(),
                        user.getName(),
                        user.getProfilePic());
            } else {
                throw new ApiRequestException("Invalid Google ID Token", HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            throw new ApiRequestException("Google authentication failed: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }
}
