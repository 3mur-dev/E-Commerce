package com.omar.ecommerce.repositories;

import com.omar.ecommerce.entities.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class CategoryRepositoryIT {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void existsByNameIgnoreCase_returnsTrueForDifferentCaseInput() {
        Category category = new Category();
        category.setName("Accessories");
        categoryRepository.save(category);

        assertTrue(categoryRepository.existsByNameIgnoreCase("aCcEsSoRiEs"));
    }
}
