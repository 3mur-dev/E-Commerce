package com.omar.ecommerce.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Entity
@Data
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Min(1)
    private int quantity;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    private Cart cart;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;
}
