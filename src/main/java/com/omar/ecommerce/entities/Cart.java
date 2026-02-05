package com.omar.ecommerce.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Cart {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    @OneToOne(cascade = CascadeType.ALL)
    User user;

    @OneToMany(mappedBy="cart", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<CartItem> items;

}
