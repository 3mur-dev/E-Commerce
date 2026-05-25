package com.omar.ecommerce.repositories;

import com.omar.ecommerce.entities.Order;
import com.omar.ecommerce.entities.PaymentStatus;
import com.omar.ecommerce.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    interface UserOrderStats {
        Long getUserId();

        Long getOrderCount();

        BigDecimal getTotalSpent();
    }

    Optional<Order> findTopByUserOrderByIdDesc(User user);

    List<Order> findByUserId(Long userId);

    Optional<Order> findByCheckoutIdempotencyKey(String checkoutIdempotencyKey);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.user.id = :userId ORDER BY o.id DESC")
    List<Order> findByUserIdWithItems(@Param("userId") Long userId);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.id = :id AND o.user.id = :userId")
    Optional<Order> findByIdAndUserIdWithItems(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.user LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product")
    List<Order> findAllWithUserAndItems();

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.user LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.id = :id")
    Optional<Order> findByIdWithUserAndItems(@Param("id") Long id);

    @Query("""
            SELECT COALESCE(SUM(o.total), 0)
            FROM Order o
            WHERE o.paymentStatus = :paymentStatus
            """)
    BigDecimal getTotalRevenue(@Param("paymentStatus") PaymentStatus paymentStatus);

    @Query("""
            SELECT COALESCE(SUM(o.total), 0)
            FROM Order o
            WHERE o.paymentStatus = :paymentStatus
              AND o.createdAt >= :from
              AND o.createdAt < :to
            """)
    BigDecimal getRevenueBetween(@Param("paymentStatus") PaymentStatus paymentStatus,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);
    @Query("""
SELECT DISTINCT o FROM Order o
JOIN FETCH o.user
JOIN FETCH o.items i
JOIN FETCH i.product
WHERE o.user.id = :userId
""")
    List<Order> findFullOrdersByUserId(@Param("userId") Long userId);

    @Query("""
SELECT DISTINCT o
FROM Order o
JOIN FETCH o.items oi
JOIN FETCH oi.product
JOIN FETCH o.user
WHERE o.id = :id AND o.user.id = :userId
""")
    Optional<Order> findFullOrderByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("""
            SELECT o.user.id AS userId,
                   COUNT(o) AS orderCount,
                   COALESCE(SUM(o.total), 0) AS totalSpent
            FROM Order o
            WHERE o.user.id IN :userIds
            GROUP BY o.user.id
            """)
    List<UserOrderStats> findUserOrderStats(@Param("userIds") Collection<Long> userIds);
}
