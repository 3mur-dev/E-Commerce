package com.omar.ecommerce.util;

import com.omar.ecommerce.dtos.ProductSearchRequest;
import com.omar.ecommerce.entities.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<Product> build(ProductSearchRequest req) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Always exclude deleted products
            predicates.add(cb.or(
                    cb.isFalse(root.get("deleted")),
                    cb.isNull(root.get("deleted"))
            ));

            // Keyword (name)
            if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("name")),
                        "%" + req.getKeyword().toLowerCase() + "%"
                ));
            }

            // Category (by ID or name)
            if (req.getCategoryId() != null) {
                predicates.add(cb.equal(
                        root.get("category").get("id"),
                        req.getCategoryId()
                ));
            }

            // Price (BigDecimal!)
            if (req.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("price"),
                        req.getMinPrice()
                ));
            }

            if (req.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("price"),
                        req.getMaxPrice()
                ));
            }

            // In stock
            if (Boolean.TRUE.equals(req.getInStock())) {
                predicates.add(cb.greaterThan(root.get("stock"), 0));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
