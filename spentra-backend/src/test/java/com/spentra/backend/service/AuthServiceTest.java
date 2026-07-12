package com.spentra.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.spentra.backend.exception.ApiRequestException;
import com.spentra.backend.model.dto.auth.LoginRequest;
import com.spentra.backend.model.dto.auth.LoginResponse;
import com.spentra.backend.model.dto.auth.SignupRequest;
import com.spentra.backend.model.dto.auth.SignupResponse;
import com.spentra.backend.model.entity.User;
import com.spentra.backend.repository.UserRepository;
import com.spentra.backend.security.JwtService;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void testSignUp_Success() {
        // Arrange
        SignupRequest request = new SignupRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("Password123");
        request.setConfirmPassword("Password123");

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setName("John Doe");
        savedUser.setEmail("john@example.com");
        savedUser.setPassword("encodedPassword123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser.getId().toString(), request.getEmail())).thenReturn("dummy_jwt_token");

        // Act
        SignupResponse response = authService.signUp(request);

        // Assert
        assertNotNull(response);
        assertEquals("dummy_jwt_token", response.getToken());
        assertEquals("John Doe", response.getName());
        assertEquals("john@example.com", response.getEmail());

        verify(userRepository).existsByEmail(request.getEmail());
        verify(passwordEncoder).encode(request.getPassword());
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken(savedUser.getId().toString(), request.getEmail());
    }

    @Test
    void testSignUp_PasswordMismatch() {
        // Arrange
        SignupRequest request = new SignupRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("Password123");
        request.setConfirmPassword("Different123");

        // Act & Assert
        ApiRequestException exception = assertThrows(ApiRequestException.class, () -> {
            authService.signUp(request);
        });

        assertEquals("Passwords do not match!", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());

        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testSignUp_UserAlreadyExists() {
        // Arrange
        SignupRequest request = new SignupRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("Password123");
        request.setConfirmPassword("Password123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // Act & Assert
        ApiRequestException exception = assertThrows(ApiRequestException.class, () -> {
            authService.signUp(request);
        });

        assertEquals("User Already Exists", exception.getMessage());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());

        verify(userRepository).existsByEmail(request.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLogin_Success() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("Password123");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("John Doe");
        user.setEmail("john@example.com");
        user.setPassword("encodedPassword123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(user.getId().toString(), user.getEmail())).thenReturn("dummy_jwt_token");

        // Act
        LoginResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("dummy_jwt_token", response.getToken());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("John Doe", response.getName());

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository).findByEmail(request.getEmail());
        verify(passwordEncoder).matches(request.getPassword(), user.getPassword());
        verify(jwtService).generateToken(user.getId().toString(), user.getEmail());
    }

    @Test
    void testLogin_UserNotFound() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("notfound@example.com");
        request.setPassword("Password123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

        // Act & Assert
        ApiRequestException exception = assertThrows(ApiRequestException.class, () -> {
            authService.login(request);
        });

        assertEquals("User not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void testLogin_UserExistsButFindByEmailReturnsEmpty() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("Password123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        ApiRequestException exception = assertThrows(ApiRequestException.class, () -> {
            authService.login(request);
        });

        assertEquals("User not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository).findByEmail(request.getEmail());
    }

    @Test
    void testLogin_InvalidPassword() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("WrongPassword");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("John Doe");
        user.setEmail("john@example.com");
        user.setPassword("encodedPassword123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

        // Act & Assert
        ApiRequestException exception = assertThrows(ApiRequestException.class, () -> {
            authService.login(request);
        });

        assertEquals("Login failed", exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository).findByEmail(request.getEmail());
        verify(passwordEncoder).matches(request.getPassword(), user.getPassword());
        verify(jwtService, never()).generateToken(anyString(), anyString());
    }
}
