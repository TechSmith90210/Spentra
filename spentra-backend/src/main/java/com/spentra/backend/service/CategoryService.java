package com.spentra.backend.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.spentra.backend.model.dto.category.CategoryRequest;
import com.spentra.backend.model.dto.category.CategoryResponse;
import com.spentra.backend.model.entity.Category;
import com.spentra.backend.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository repo;

    public CategoryResponse addCategory(CategoryRequest req) {
        Category category = new Category();

        category.setName(req.getName());
        Category result = repo.save(category);

        return new CategoryResponse(
                result.getId(),
                result.getName());
    }

    public List<CategoryResponse> getCategories() {
        List<Category> fetchedCategories = repo.findAll();

        List<CategoryResponse> categoryResponses = fetchedCategories.stream().map(
                category -> new CategoryResponse(category.getId(), category.getName())).collect(Collectors.toList());

        return categoryResponses;
    }

    public CategoryResponse updateCategory(CategoryRequest req, UUID id) {
        Category fetchedCategory = repo.findById(id).orElseThrow(
                () -> new RuntimeException(
                        "cannot find a category with this id, check id again"));
        if (req == null) {
            throw new RuntimeException("add things to update");
        }

        if (req.getName() != null) {
            fetchedCategory.setName(req.getName());
        }

        Category savedCategory = repo.save(fetchedCategory);

        return new CategoryResponse(savedCategory.getId(), savedCategory.getName());
    }

    public void deleteCategory(UUID id) {
        Category fetchedCategory = repo.findById(id).orElseThrow(
                () -> new RuntimeException("cant find a category with this id to delete, check again"));

        repo.delete(fetchedCategory);
        System.out.println("deleted the category: " + id);
    }
}
