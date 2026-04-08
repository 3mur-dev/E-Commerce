package com.omar.ecommerce.services;

import com.omar.ecommerce.dtos.CategoryRequest;
import com.omar.ecommerce.entities.Category;
import com.omar.ecommerce.entities.Product;
import com.omar.ecommerce.repositories.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void add_whenRequestIsValid_trimsNameBeforeSaving() {
        CategoryRequest request = new CategoryRequest();
        request.setName("  Laptops  ");

        when(categoryRepository.existsByNameIgnoreCase("Laptops")).thenReturn(false);
        when(categoryRepository.save(org.mockito.ArgumentMatchers.any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Category created = categoryService.add(request);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertEquals("Laptops", captor.getValue().getName());
        assertEquals("Laptops", created.getName());
    }

    @Test
    void add_whenCategoryExists_throwsException() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Phones");
        when(categoryRepository.existsByNameIgnoreCase("Phones")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> categoryService.add(request));
        assertEquals("Category already exists", ex.getMessage());
    }

    @Test
    void delete_whenCategoryHasProducts_throwsException() {
        Category category = new Category();
        category.setId(10L);
        category.setProducts(List.of(new Product()));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> categoryService.delete(10L));
        assertTrue(ex.getMessage().contains("Cannot delete category with products"));
    }
}
