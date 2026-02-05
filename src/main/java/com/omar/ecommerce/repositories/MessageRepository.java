package com.omar.ecommerce.repositories;

import com.omar.ecommerce.entities.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
}
