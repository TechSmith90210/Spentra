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

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private CategoryService service;

    public CategoryController(CategoryService serve) {
        this.service = serve;
    }

    @GetMapping
    public List<CategoryResponse> getAllCategories() {
        return service.getCategories();
    }

    @PostMapping
    public CategoryResponse addCategory(@RequestBody CategoryRequest req) {
        return service.addCategory(req);
    }

    @PatchMapping("/{id}")
    public CategoryResponse updateCategory(@RequestBody CategoryRequest req, @PathVariable UUID id) {
        return service.updateCategory(req, id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        service.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
