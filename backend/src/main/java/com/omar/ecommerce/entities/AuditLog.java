package com.omar.ecommerce.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Action is required")
    @Size(max = 100)
    @Column(nullable = false)
    private String action;

    @NotBlank(message = "Entity type is required")
    @Size(max = 100)
    @Column(nullable = false)
    private String entityType;

    @NotNull(message = "Entity ID is required")
    @Column(nullable = false)
    private Long entityId;

    @NotBlank(message = "Performed by is required")
    @Size(max = 255)
    @Column(nullable = false)
    private String performedBy;

    @Size(max = 45)
    private String ipAddress;

    @Size(max = 2000)
    private String userAgent;

    @Size(max = 2000)
    @Column(length = 2000)
    private String details;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}