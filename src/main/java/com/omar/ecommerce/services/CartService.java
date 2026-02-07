package com.omar.ecommerce.services;

import com.omar.ecommerce.entities.Cart;
import com.omar.ecommerce.entities.CartItem;
import com.omar.ecommerce.entities.Product;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.CartItemRepository;
import com.omar.ecommerce.repositories.CartRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Transactional
@AllArgsConstructor
public class CartService {

    private CartRepository cartRepository;
    private CartItemRepository cartItemRepository;

    @Transactional
    public Cart getOrCreateCart(User user) {
        if (user == null) {
            throw new IllegalStateException("User must not be null when creating cart");
        }

        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }


    public CartItem addToCart(Product product, User user) {

        Cart cart = getOrCreateCart(user);

        Optional<CartItem> itemOpt = cartItemRepository.findByCartAndProduct(cart, product);

        if (itemOpt.isPresent()) {

            CartItem existingItem = itemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + 1);
            cartItemRepository.save(existingItem);
            return existingItem;

        }else  {
            CartItem cartItem = new CartItem();
            cartItem.setProduct(product);
            cartItem.setCart(cart);
            cartItem.setQuantity(1);
            return cartItemRepository.save(cartItem);
        }
    }
    public void decreaseQuantity(User user, Product product){

        Cart cart = getOrCreateCart(user);
        Optional<CartItem> itemOpt = cartItemRepository.findByCartAndProduct(cart, product);

        if (itemOpt.isPresent()) {
            CartItem existingItem = itemOpt.get();

            if(existingItem.getQuantity() > 1){
                existingItem.setQuantity(existingItem.getQuantity() - 1);
                cartItemRepository.save(existingItem);
            }else {

                cartItemRepository.delete(existingItem);
            }
        }
    }
    public BigDecimal calculateTotalPrice(Cart cart) {
        if (cart == null || cart.getItems() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            // Ensure product and price are not null before calculating
            if (item.getProduct() != null && item.getProduct().getPrice() != null) {
                BigDecimal itemPrice = item.getProduct().getPrice();
                // Multiply the item's price by its quantity
                BigDecimal itemTotal = itemPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                // Add this item's total to the running cart total
                total = total.add(itemTotal);
            }
        }
        return total;
    }

    // Alternative method using Java Streams
    public BigDecimal calculateTotalPriceStreams(Cart cart) {
        if (cart == null || cart.getItems() == null) {
            return BigDecimal.ZERO;
        }

        return cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}