package com.spentra.backend.service;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.spentra.backend.exception.ApiRequestException;
import com.spentra.backend.model.dto.user.UserProfileRequest;
import com.spentra.backend.model.dto.user.UserProfileResponse;
import com.spentra.backend.model.entity.User;
import com.spentra.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Helper method to retrieve the currently authenticated user from SecurityContext.
     */
    public User getCurrentUser() {
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (userIdStr == null) {
            throw new ApiRequestException("Unauthenticated request", HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> new ApiRequestException("User not found", HttpStatus.UNAUTHORIZED));
    }

    /**
     * Get user profile details.
     */
    public UserProfileResponse getProfile() {
        User user = getCurrentUser();
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getProfilePic()
        );
    }

    /**
     * Update user profile (name, profile picture).
     */
    public UserProfileResponse updateProfile(UserProfileRequest req) {
        User user = getCurrentUser();

        if (req.getName() != null) {
            user.setName(req.getName());
        }

        if (req.getProfilePic() != null) {
            user.setProfilePic(req.getProfilePic());
        }

        User savedUser = userRepository.save(user);

        return new UserProfileResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getProfilePic()
        );
    }
}
