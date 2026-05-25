package com.omar.ecommerce.services;

import com.omar.ecommerce.dtos.CartItemDTO;
import com.omar.ecommerce.dtos.CartResponse;
import com.omar.ecommerce.dtos.QuantityRequest;
import com.omar.ecommerce.entities.Cart;
import com.omar.ecommerce.entities.CartItem;
import com.omar.ecommerce.entities.Product;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.CartItemRepository;
import com.omar.ecommerce.repositories.CartRepository;
import com.omar.ecommerce.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

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

    public CartResponse getCart(User user) {
        return toResponse(getOrCreateCart(user));
    }

    @Transactional
    public CartItem addToCart(Product product, User user) {
        return addToCart(user, product, 1);
    }
    @Transactional
    public CartItem addToCart(User user, Product product, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (product == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        if (Boolean.TRUE.equals(product.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }

        Cart cart = getOrCreateCart(user);
        Optional<CartItem> itemOpt = cartItemRepository.findByCartAndProduct(cart, product);
        int currentQuantity = itemOpt.map(CartItem::getQuantity).orElse(0);
        int updatedQuantity = currentQuantity + quantity;

        if (updatedQuantity > product.getStock()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Requested quantity exceeds available stock");
        }

        CartItem item = itemOpt.orElseGet(CartItem::new);
        item.setProduct(product);
        item.setCart(cart);
        item.setQuantity(updatedQuantity);
        return cartItemRepository.save(item);
    }

    @Transactional
    public CartResponse addItem(User user, CartItemDTO request) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (request == null || request.getProductId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product id is required");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        if (Boolean.TRUE.equals(product.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }

        int quantityToAdd = request.getQuantity() > 0 ? request.getQuantity() : 1;
        addToCart(user, product, quantityToAdd);
        return toResponse(getOrCreateCart(user));
    }

    public CartResponse updateQuantity(User user, Long cartItemId, QuantityRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity request is required");
        }
        updateItemQuantity(user, cartItemId, request.getQuantity());
        return toResponse(getOrCreateCart(user));
    }

    public CartResponse removeItem(User user, Long cartItemId) {
        removeCartItem(user, cartItemId);
        return toResponse(getOrCreateCart(user));
    }

    public CartItem updateItemQuantity(User user, Long cartItemId, int quantity) {
        if (quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be at least 1");
        }

        Cart cart = getOrCreateCart(user);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your cart item");
        }

        int stock = item.getProduct().getStock();
        if (quantity > stock) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough stock");
        }

        item.setQuantity(quantity);
        return cartItemRepository.save(item);
    }

    public void removeCartItem(User user, Long cartItemId) {
        Cart cart = getOrCreateCart(user);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your cart item");
        }

        cartItemRepository.delete(item);
    }

    public List<CartItem> getCartItems(Cart cart) {
        return cartItemRepository.findByCart(cart);
    }

    public List<CartItem> getCartItems(User user) {
        return getCartItems(getOrCreateCart(user));
    }

    public BigDecimal calculateTotalPrice(Cart cart) {
        if (cart == null || cart.getItems() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            if (item.getProduct() != null && item.getProduct().getPrice() != null) {
                BigDecimal itemTotal = item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                total = total.add(itemTotal);
            }
        }
        return total;
    }

    public void decreaseQuantity(User user, Product product) {
        Cart cart = getOrCreateCart(user);
        Optional<CartItem> itemOpt = cartItemRepository.findByCartAndProduct(cart, product);

        if (itemOpt.isPresent()) {
            CartItem existingItem = itemOpt.get();

            if (existingItem.getQuantity() > 1) {
                existingItem.setQuantity(existingItem.getQuantity() - 1);
                cartItemRepository.save(existingItem);
            } else {
                cartItemRepository.delete(existingItem);
            }
        }
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItem> items = getCartItems(cart);
        List<CartItemDTO> mappedItems = items.stream().map(item -> {
            CartItemDTO dto = new CartItemDTO();
            dto.setId(item.getId());
            dto.setProductId(item.getProduct().getId());
            dto.setProductName(item.getProduct().getName());
            dto.setImageUrl(item.getProduct().getImageUrl());
            dto.setPrice(item.getProduct().getPrice());
            dto.setQuantity(item.getQuantity());
            dto.setStock(item.getProduct().getStock());
            BigDecimal subtotal = item.getProduct().getPrice() == null
                    ? BigDecimal.ZERO
                    : item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            dto.setSubtotal(subtotal);
            return dto;
        }).toList();

        BigDecimal total = mappedItems.stream()
                .map(item -> item.getSubtotal() == null ? BigDecimal.ZERO : item.getSubtotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CartResponse response = new CartResponse();
        response.setItems(mappedItems);
        response.setTotal(total);
        return response;
    }
}
