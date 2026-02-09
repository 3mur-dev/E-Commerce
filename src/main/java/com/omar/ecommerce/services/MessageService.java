package com.omar.ecommerce.services;

import com.omar.ecommerce.dtos.MessageRequest;
import com.omar.ecommerce.entities.Message;
import com.omar.ecommerce.repositories.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository repository;

    @Transactional // Ensures database integrity
    public Message save(MessageRequest request) {

        // Map Request DTO to Entity
        Message message = new Message();
        message.setName(request.getName());
        message.setEmail(request.getEmail());
        message.setMessage(request.getMessage());

        // Save the entity
        Message savedMessage = repository.save(message);

        // Return the saved object so you have access to 'id' and 'createdAt'
        return savedMessage;

    }
}

