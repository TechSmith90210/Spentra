package com.spentra.backend.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.spentra.backend.exception.ApiRequestException;
import com.spentra.backend.model.dto.category.CategoryRequest;
import com.spentra.backend.model.dto.category.CategoryResponse;
import com.spentra.backend.model.entity.Category;
import com.spentra.backend.model.entity.User;
import com.spentra.backend.repository.CategoryRepository;
import com.spentra.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service class responsible for category operations.
 * Handles category creation, retrieval, updates, and deletion with user isolation.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository repo;
    private final UserRepository userRepository;

    /**
     * Helper method to retrieve the currently authenticated user from SecurityContext.
     *
     * @return User the authenticated user entity
     * @throws ApiRequestException if the user is not found or unauthenticated
     */
    private User getCurrentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            throw new ApiRequestException("Unauthenticated request", HttpStatus.UNAUTHORIZED);
        }
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (userIdStr == null) {
            throw new ApiRequestException("Unauthenticated request", HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> new ApiRequestException("User not found", HttpStatus.UNAUTHORIZED));
    }

    /**
     * Creates a new category and binds it to the currently authenticated user.
     *
     * @param req category request payload
     * @return CategoryResponse details of the created category
     */
    public CategoryResponse addCategory(CategoryRequest req) {
        if (req == null || req.getName() == null || req.getName().trim().isEmpty()) {
            throw new ApiRequestException("Category name must not be empty", HttpStatus.BAD_REQUEST);
        }

        User currentUser = getCurrentUser();
        Category category = new Category();
        category.setName(req.getName());
        category.setUser(currentUser);

        Category result = repo.save(category);

        return new CategoryResponse(
                result.getId(),
                result.getName());
    }

    /**
     * Retrieves all categories accessible to the currently authenticated user.
     * This includes global categories (where user is null) and user-specific categories.
     *
     * @return List of CategoryResponse objects
     */
    public List<CategoryResponse> getCategories() {
        User currentUser = getCurrentUser();
        List<Category> fetchedCategories = repo.findByUserIdOrUserIsNull(currentUser.getId());

        return fetchedCategories.stream()
                .map(category -> new CategoryResponse(category.getId(), category.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Updates an existing category for the currently authenticated user.
     * Users are blocked from updating global categories or categories belonging to other users.
     *
     * @param req category update payload
     * @param id ID of the category to update
     * @return CategoryResponse details of the updated category
     */
    public CategoryResponse updateCategory(CategoryRequest req, UUID id) {
        Category fetchedCategory = repo.findById(id)
                .orElseThrow(() -> new ApiRequestException("Category not found with the specified ID", HttpStatus.NOT_FOUND));

        User currentUser = getCurrentUser();

        // Enforce user isolation
        if (fetchedCategory.getUser() == null) {
            throw new ApiRequestException("Cannot modify global/system categories", HttpStatus.FORBIDDEN);
        }
        if (!fetchedCategory.getUser().getId().equals(currentUser.getId())) {
            throw new ApiRequestException("Access denied: You do not own this category", HttpStatus.FORBIDDEN);
        }

        if (req == null || req.getName() == null || req.getName().trim().isEmpty()) {
            throw new ApiRequestException("Category name must not be empty", HttpStatus.BAD_REQUEST);
        }

        fetchedCategory.setName(req.getName());
        Category savedCategory = repo.save(fetchedCategory);

        return new CategoryResponse(savedCategory.getId(), savedCategory.getName());
    }

    /**
     * Deletes a category belonging to the currently authenticated user.
     * Users are blocked from deleting global categories or categories belonging to other users.
     *
     * @param id ID of the category to delete
     */
    public void deleteCategory(UUID id) {
        Category fetchedCategory = repo.findById(id)
                .orElseThrow(() -> new ApiRequestException("Category not found with the specified ID", HttpStatus.NOT_FOUND));

        User currentUser = getCurrentUser();

        // Enforce user isolation
        if (fetchedCategory.getUser() == null) {
            throw new ApiRequestException("Cannot delete global/system categories", HttpStatus.FORBIDDEN);
        }
        if (!fetchedCategory.getUser().getId().equals(currentUser.getId())) {
            throw new ApiRequestException("Access denied: You do not own this category", HttpStatus.FORBIDDEN);
        }

        repo.delete(fetchedCategory);
    }
}

