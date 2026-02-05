package com.omar.ecommerce.entities;

import jakarta.persistence.*; // Correct for modern Spring Boot
import lombok.*;
import org.hibernate.annotations.CreationTimestamp; // Needed to automate the variable
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
public class Message {
    @Id // Ensure this is from jakarta.persistence
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String message;

    @CreationTimestamp // Automatically sets the time on first save
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;
}
