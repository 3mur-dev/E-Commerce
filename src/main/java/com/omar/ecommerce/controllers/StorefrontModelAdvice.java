package com.omar.ecommerce.controllers;

import com.omar.ecommerce.entities.CartItem;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.CartItemRepository;
import com.omar.ecommerce.repositories.CartRepository;
import com.omar.ecommerce.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class StorefrontModelAdvice {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @ModelAttribute("cartCount")
    public int cartCount(Authentication authentication) {
        User user = resolveUser(authentication);
        if (user == null) {
            return 0;
        }

        return cartRepository.findByUser(user)
                .map(cart -> cartItemRepository.findByCart(cart).stream().mapToInt(CartItem::getQuantity).sum())
                .orElse(0);
    }

    @ModelAttribute("currentUsername")
    public String currentUsername(Authentication authentication) {
        User user = resolveUser(authentication);
        return user != null ? user.getUsername() : null;
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        String username = null;
        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else if (principal instanceof String principalText && !"anonymousUser".equals(principalText)) {
            username = principalText;
        }

        if (username == null || username.isBlank()) {
            return null;
        }

        return userRepository.findByUsername(username).orElse(null);
    }
}
