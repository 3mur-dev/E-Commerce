package com.omar.ecommerce.dtos;

import com.omar.ecommerce.entities.Product;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String shortDescription;
    private String longDescription;
    private BigDecimal price;

    private Long categoryId;
    private String categoryName;

    private int stock;
    private String imageUrl;
    private Boolean deleted;

    private boolean isFavorited;

    public ProductResponse(ProductResponse other) {
        if (other == null) {
            return;
        }

        this.id = other.id;
        this.name = other.name;
        this.shortDescription = other.shortDescription;
        this.longDescription = other.longDescription;
        this.price = other.price;
        this.categoryId = other.categoryId;
        this.categoryName = other.categoryName;
        this.stock = other.stock;
        this.imageUrl = other.imageUrl;
        this.deleted = other.deleted;
        this.isFavorited = other.isFavorited;
    }

    public ProductResponse copy() {
        return new ProductResponse(this);
    }
}
