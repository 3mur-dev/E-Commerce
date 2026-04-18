package com.omar.ecommerce.mapper;

import com.omar.ecommerce.dtos.ProductResponse;
import com.omar.ecommerce.entities.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        if (product == null) return null;

        ProductResponse res = new ProductResponse();

        res.setId(product.getId());
        res.setName(product.getName());
        res.setPrice(product.getPrice());
        res.setStock(product.getStock());
        res.setImageUrl(product.getImageUrl());


        res.setCategoryId(product.getCategory().getId());
        res.setCategoryName(product.getCategory().getName());

        return res;
    }
}
