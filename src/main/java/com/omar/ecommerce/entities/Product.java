package com.omar.ecommerce.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@SQLDelete(sql = "UPDATE \"Product\" SET \"deleted\" = true WHERE \"id\" = ?")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imageUrl;

    private String name;

    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column
    private Boolean deleted = Boolean.FALSE; // soft delete flag
}
