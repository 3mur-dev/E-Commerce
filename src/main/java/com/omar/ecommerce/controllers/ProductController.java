package com.omar.ecommerce.controllers;


import com.omar.ecommerce.dtos.ProductResponse;
import com.omar.ecommerce.dtos.ProductSearchRequest;
import com.omar.ecommerce.entities.Category;
import com.omar.ecommerce.services.CategoryService;
import com.omar.ecommerce.services.FavoriteService;
import com.omar.ecommerce.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final FavoriteService favoriteService;
    private final CategoryService categoryService;

    @GetMapping
    public String getAllProducts(Model model,
                                 ProductSearchRequest request,
                                 @AuthenticationPrincipal UserDetails userDetails) {

        Page<ProductResponse> productPage = productService.search(request);
        markFavorites(productPage, userDetails);
        List<Category> categories = categoryService.findAll();

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("productPage", productPage);
        model.addAttribute("currentPage", productPage.getNumber());
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalElements", productPage.getTotalElements());
        model.addAttribute("categories", categories);
        model.addAttribute("searchRequest", request);
        model.addAttribute("keyword", request.getKeyword());
        model.addAttribute("selectedCategoryId", request.getCategoryId());
        model.addAttribute("minPrice", request.getMinPrice());
        model.addAttribute("maxPrice", request.getMaxPrice());
        model.addAttribute("inStock", Boolean.TRUE.equals(request.getInStock()));
        model.addAttribute("sortBy", request.getSortBy());
        model.addAttribute("sortDir", request.getSortDir());

        return "products";
    }

    private void markFavorites(Page<ProductResponse> productPage, UserDetails userDetails) {
        if (userDetails == null) {
            return;
        }

        Set<Long> favoriteIds = new HashSet<>(favoriteService.findUserFavorites(userDetails.getUsername()));
        productPage.getContent().forEach(product ->
                product.setFavorited(favoriteIds.contains(product.getId())));
    }

    // DETAIL PAGE
    @GetMapping("/{id}")
    public String getProductDetails(@PathVariable Long id,
                                    Model model,
                                    @AuthenticationPrincipal UserDetails userDetails) {

        ProductResponse product = productService.findById(id);

        if (userDetails != null) {
            Set<Long> favorites = new HashSet<>(
                    favoriteService.findUserFavorites(userDetails.getUsername())
            );

            product.setFavorited(favorites.contains(id));
        }

        model.addAttribute("product", product);

        return "products/details";
    }
}
