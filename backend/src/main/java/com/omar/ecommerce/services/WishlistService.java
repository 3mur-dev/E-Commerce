package com.omar.ecommerce.services;

import com.omar.ecommerce.dtos.WishlistItemResponse;
import com.omar.ecommerce.dtos.WishlistResponse;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.entities.Wishlist;
import com.omar.ecommerce.entities.WishlistItem;
import com.omar.ecommerce.entities.WishlistVisibility;
import com.omar.ecommerce.repositories.ProductRepository;
import com.omar.ecommerce.repositories.WishlistItemRepository;
import com.omar.ecommerce.repositories.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Wishlist getOrCreateDefaultList(User user) {
        requireUser(user);

        return wishlistRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultWishlist(user));
    }

    @Transactional(readOnly = true)
    public WishlistItem getWishlistItem(User user, Long wishlistId, Long productId) {
        requireUser(user);

        return wishlistItemRepository
                .findSecureItem(
                        wishlistId,
                        productId,
                        user.getId()
                )
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Wishlist item not found")
                );
    }

    @Transactional(readOnly = true)
    public Wishlist getUserWishlist(User user, Long wishlistId) {
        requireUser(user);

        return wishlistRepository.findByUserIdAndId(user.getId(), wishlistId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Wishlist not found")
                );
    }

    @Transactional(readOnly = true)
    public WishlistResponse getWishlist(User user) {
        requireUser(user);

        Wishlist wishlist = getOrCreateDefaultList(user);
        return toResponse(wishlist);
    }

    @Transactional
    public WishlistResponse deleteWishlistItem(User user, Long wishlistId, Long productId) {
        requireUser(user);

        WishlistItem item = getWishlistItem(user, wishlistId, productId);
        wishlistItemRepository.delete(item);

        return toResponse(getUserWishlist(user, wishlistId));
    }

    @Transactional
    public boolean toggleWishlistItem(User user, Long productId) {
        requireUser(user);
        if (productId == null || productId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product id is required");
        }

        Wishlist wishlist = getOrCreateDefaultList(user);
        WishlistItem item = wishlistItemRepository.findByWishlistIdAndProductId(wishlist.getId(), productId)
                .orElse(null);
        if (item != null) {
            wishlistItemRepository.delete(item);
            return false;
        }

        WishlistItem wishlistItem = new WishlistItem();
        wishlistItem.setWishlist(wishlist);
        wishlistItem.setProduct(productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product with ID " + productId + " not found")));
        wishlistItem.setDesiredQuantity(1);
        wishlistItemRepository.save(wishlistItem);
        return true;
    }

    private Wishlist createDefaultWishlist(User user) {
        Wishlist wishlist = new Wishlist();
        wishlist.setName("Wishlist");
        wishlist.setUser(user);
        wishlist.setVisibility(WishlistVisibility.PRIVATE);
        wishlist.setDefaultList(true);

        return wishlistRepository.save(wishlist);
    }

    private void requireUser(User user) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }

    private WishlistResponse toResponse(Wishlist wishlist) {
        WishlistResponse response = new WishlistResponse();

        response.setId(wishlist.getId());
        String name = wishlist.getName();
        String resolvedName = name == null || name.isBlank() ? "Wishlist" : name;
        response.setName(resolvedName);
        response.setVisibility(wishlist.getVisibility());
        response.setDefaultList(wishlist.isDefaultList() || "Wishlist".equalsIgnoreCase(resolvedName));
        response.setShareToken(wishlist.getShareToken());

        if (wishlist.getUser() != null) {
            response.setUserId(wishlist.getUser().getId());
            response.setUsername(wishlist.getUser().getUsername());
        }

        List<WishlistItem> items =
                wishlistItemRepository.findByWishlistIdWithProduct(wishlist.getId());

        response.setItems(
                items.stream()
                        .map(this::toItemResponse)
                        .toList()
        );

        return response;
    }

    private WishlistItemResponse toItemResponse(WishlistItem item) {
        WishlistItemResponse response = new WishlistItemResponse();

        response.setId(item.getId());
        response.setDesiredQuantity(item.getDesiredQuantity());
        response.setAddedAt(item.getAddedAt());

        if (item.getProduct() != null) {
            response.setProductId(item.getProduct().getId());
            response.setProductName(item.getProduct().getName());
            response.setImageUrl(item.getProduct().getImageUrl());
            response.setPrice(item.getProduct().getPrice());
        }

        return response;
    }
}
