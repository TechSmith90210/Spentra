package com.spentra.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.spentra.backend.exception.ApiRequestException;
import com.spentra.backend.model.dto.user.UserProfileRequest;
import com.spentra.backend.model.dto.user.UserProfileResponse;
import com.spentra.backend.model.entity.User;
import com.spentra.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");
        testUser.setName("Initial Name");
        testUser.setProfilePic("initial-pic-url");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuthentication() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userId.toString());
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testGetProfile_Success() {
        // Arrange
        mockAuthentication();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act
        UserProfileResponse response = userService.getProfile();

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("Initial Name", response.getName());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("initial-pic-url", response.getProfilePic());

        verify(userRepository).findById(userId);
    }

    @Test
    void testGetProfile_UserNotFound() {
        // Arrange
        mockAuthentication();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ApiRequestException.class, () -> userService.getProfile());
        verify(userRepository).findById(userId);
    }

    @Test
    void testUpdateProfile_Success() {
        // Arrange
        mockAuthentication();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileRequest request = new UserProfileRequest();
        request.setName("Updated Name");
        request.setProfilePic("new-pic-url");

        // Act
        UserProfileResponse response = userService.updateProfile(request);

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("Updated Name", response.getName());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("new-pic-url", response.getProfilePic());

        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testGetCurrentUser_NullAuthenticationThrowsUnauthorized() {
        // Arrange
        SecurityContextHolder.clearContext();

        // Act & Assert
        ApiRequestException exception = assertThrows(ApiRequestException.class, () -> userService.getCurrentUser());
        assertEquals(org.springframework.http.HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Unauthenticated request", exception.getMessage());
    }
}
