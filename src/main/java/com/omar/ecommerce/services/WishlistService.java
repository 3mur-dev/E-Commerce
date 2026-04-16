package com.omar.ecommerce.services;

import com.omar.ecommerce.entities.Favorite;
import com.omar.ecommerce.entities.Product;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.entities.Wishlist;
import com.omar.ecommerce.entities.WishlistItem;
import com.omar.ecommerce.entities.WishlistVisibility;
import com.omar.ecommerce.repositories.FavoriteRepository;
import com.omar.ecommerce.repositories.ProductRepository;
import com.omar.ecommerce.repositories.WishlistItemRepository;
import com.omar.ecommerce.repositories.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private static final String DEFAULT_LIST_NAME = "Wishlist";

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final FavoriteRepository favoriteRepository;

    public List<Wishlist> getUserWishlists(Long userId) {
        return wishlistRepository.findByUserIdOrderByIdAsc(userId);
    }

    @Transactional
    public Wishlist getOrCreateDefaultList(User user) {
        Optional<Wishlist> existing = wishlistRepository.findByUserIdAndDefaultListTrue(user.getId());
        if (existing.isPresent()) {
            Wishlist wishlist = existing.get();
            syncFavoritesToDefault(user, wishlist);
            mergeOtherLists(user, wishlist);
            return wishlist;
        }

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setName(DEFAULT_LIST_NAME);
        wishlist.setDefaultList(true);
        wishlist.setVisibility(WishlistVisibility.PRIVATE);
        Wishlist saved = wishlistRepository.save(wishlist);
        syncFavoritesToDefault(user, saved);
        mergeOtherLists(user, saved);
        return saved;
    }

    @Transactional
    public Wishlist getUserWishlist(User user, Long wishlistId) {
        return wishlistRepository.findByUserIdAndId(user.getId(), wishlistId)
                .orElseThrow(() -> new RuntimeException("Wishlist not found"));
    }

    @Transactional
    public String ensureShareToken(User user, Long wishlistId) {
        Wishlist wishlist = getUserWishlist(user, wishlistId);
        if (wishlist.getShareToken() == null) {
            wishlist.setShareToken(generateShareToken());
            wishlistRepository.save(wishlist);
        }
        return wishlist.getShareToken();
    }

    public Wishlist getSharedWishlist(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return wishlistRepository.findByShareToken(token).orElse(null);
    }

    public List<WishlistItem> getWishlistItems(Long wishlistId) {
        return wishlistItemRepository.findByWishlistIdOrderByAddedAtDesc(wishlistId);
    }

    @Transactional
    public WishlistItem addItem(User user, Long wishlistId, Long productId,
                                Integer desiredQuantity) {
        Wishlist wishlist = getUserWishlist(user, wishlistId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Optional<WishlistItem> existing = wishlistItemRepository
                .findByWishlistIdAndProductId(wishlist.getId(), product.getId());

        WishlistItem item;
        if (existing.isPresent()) {
            item = existing.get();
        } else {
            item = new WishlistItem();
            item.setWishlist(wishlist);
            item.setProduct(product);
        }

        Integer normalizedQty = normalizeDesiredQuantity(desiredQuantity);
        if (normalizedQty != null) {
            item.setDesiredQuantity(normalizedQty);
        }

        return wishlistItemRepository.save(item);
    }
    @Transactional
    public void removeItem(User user, Long wishlistId, Long itemId) {
        Wishlist wishlist = getUserWishlist(user, wishlistId);

        // Fetch the item to delete
        WishlistItem item = wishlistItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Wishlist item not found"));

        // Ensure it belongs to the user
        if (!item.getWishlist().getId().equals(wishlist.getId())) {
            throw new RuntimeException("Wishlist item not found");
        }

        // Delete from DB
        wishlistItemRepository.delete(item);
    }

    @Transactional
    public void addToDefaultList(User user, Long productId) {
        Wishlist defaultList = getOrCreateDefaultList(user);
        addItem(user, defaultList.getId(), productId, null);
    }

    @Transactional
    public void removeFromDefaultList(User user, Long productId) {
        Wishlist defaultList = getOrCreateDefaultList(user);
        wishlistItemRepository.findByWishlistIdAndProductId(defaultList.getId(), productId)
                .ifPresent(wishlistItemRepository::delete);
    }

    @Transactional
    public void syncFavoritesToDefault(User user, Wishlist wishlist) {
        List<Favorite> favorites = favoriteRepository.findByUserId(user.getId());
        if (favorites.isEmpty()) {
            return;
        }

        Set<Long> existingProductIds = new HashSet<>(
                wishlistItemRepository.findProductIdsByWishlistId(wishlist.getId()));
        for (Favorite favorite : favorites) {
            Long productId = favorite.getProduct().getId();
            if (!existingProductIds.contains(productId)) {
                WishlistItem item = new WishlistItem();
                item.setWishlist(wishlist);
                item.setProduct(favorite.getProduct());
                item.setDesiredQuantity(1);
                wishlistItemRepository.save(item);
            }
        }
    }

    private String generateShareToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void mergeOtherLists(User user, Wishlist defaultList) {
        List<Wishlist> wishlists = wishlistRepository.findByUserIdOrderByIdAsc(user.getId());
        for (Wishlist other : wishlists) {
            if (other.getId().equals(defaultList.getId())) continue;

            List<WishlistItem> items = wishlistItemRepository.findByWishlistIdOrderByAddedAtDesc(other.getId());
            for (WishlistItem item : items) {
                mergeItemIntoDefault(item, defaultList);
            }
            wishlistRepository.delete(other);
        }
    }

    private void mergeItemIntoDefault(WishlistItem item, Wishlist defaultList) {
        Optional<WishlistItem> existing = wishlistItemRepository
                .findByWishlistIdAndProductId(defaultList.getId(), item.getProduct().getId());

        if (existing.isPresent()) {
            WishlistItem target = existing.get();
            target.setDesiredQuantity(target.getDesiredQuantity() + item.getDesiredQuantity());
            wishlistItemRepository.save(target);
            wishlistItemRepository.delete(item);
            return;
        }

        item.setWishlist(defaultList);
        wishlistItemRepository.save(item);
    }

    private Integer normalizeDesiredQuantity(Integer desiredQuantity) {
        if (desiredQuantity == null || desiredQuantity < 1) {
            return null;
        }
        return desiredQuantity;
    }
}