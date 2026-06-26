package com.spentra.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spentra.backend.model.dto.category.CategoryRequest;
import com.spentra.backend.model.dto.category.CategoryResponse;
import com.spentra.backend.service.CategoryService;

/**
 * Controller class handling HTTP requests for category management.
 * Provides endpoints to create, read, update, and delete categories.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService service;

    /**
     * Constructor for dependency injection.
     *
     * @param service the category service
     */
    public CategoryController(CategoryService service) {
        this.service = service;
    }

    /**
     * Retrieves all categories accessible to the authenticated user.
     *
     * @return List of CategoryResponse representing the accessible categories
     */
    @GetMapping
     public List<CategoryResponse> getAllCategories() {
        return service.getCategories();
    }

    /**
     * Creates a new category for the authenticated user.
     *
     * @param req category request payload
     * @return CategoryResponse details of the created category
     */
    @PostMapping
    public CategoryResponse addCategory(@RequestBody CategoryRequest req) {
        return service.addCategory(req);
    }

    /**
     * Updates an existing category's properties.
     *
     * @param req category update payload
     * @param id ID of the category to update
     * @return CategoryResponse details of the updated category
     */
    @PatchMapping("/{id}")
    public CategoryResponse updateCategory(@RequestBody CategoryRequest req, @PathVariable UUID id) {
        return service.updateCategory(req, id);
    }

    /**
     * Deletes a user-specific category.
     *
     * @param id ID of the category to delete
     * @return ResponseEntity with No Content status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        service.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}

