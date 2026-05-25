package com.omar.ecommerce.services;

import com.omar.ecommerce.dtos.CategoryRequest;
import com.omar.ecommerce.entities.Category;
import com.omar.ecommerce.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    private void validateCategoryRequest(CategoryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        if (request.getName() == null || request.getName().trim().length() < 2) {
            throw new IllegalArgumentException("Category name must be at least 2 characters");
        }
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Category add(CategoryRequest request) {
        validateCategoryRequest(request);

        String name = request.getName().trim();

        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Category already exists");
        }

        Category category = new Category();
        category.setName(name);

        return categoryRepository.save(category);
    }

    public void delete(long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (!category.getProducts().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with products");
        }

        categoryRepository.delete(category);
    }
}

