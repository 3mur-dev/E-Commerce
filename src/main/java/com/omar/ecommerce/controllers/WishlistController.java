package com.omar.ecommerce.controllers;

import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.entities.Wishlist;
import com.omar.ecommerce.entities.WishlistItem;
import com.omar.ecommerce.repositories.UserRepository;
import com.omar.ecommerce.services.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final UserRepository userRepository;
    private final WishlistService wishlistService;

    @GetMapping
    @Transactional
    public String showDefaultWishlist(Authentication auth, Model model) {
        User user = getUser(auth);
        if (user == null) {
            return "redirect:/login";
        }

        Wishlist defaultWishlist = wishlistService.getOrCreateDefaultList(user);
        return renderWishlist(model, user, defaultWishlist);
    }

    @GetMapping("/{id}")
    public String redirectLegacyList(Authentication auth, @PathVariable Long id) {
        if (getUser(auth) == null) {
            return "redirect:/login";
        }
        return "redirect:/wishlists";
    }

    @PostMapping("/share")
    public String shareWishlist(Authentication auth) {
        User user = getUser(auth);
        if (user == null) {
            return "redirect:/login";
        }

        Wishlist wishlist = wishlistService.getOrCreateDefaultList(user);
        wishlistService.ensureShareToken(user, wishlist.getId());
        return "redirect:/wishlists";
    }

    @PostMapping("/items/remove")
    public String removeItem(Authentication auth,
                             @RequestParam Long itemId,
                             @RequestParam Long listId) {
        User user = getUser(auth);
        if (user == null) {
            return "redirect:/login";
        }

        wishlistService.removeItem(user, listId, itemId);
        return "redirect:/wishlists";
    }

    @GetMapping("/shared/{token}")
    public String viewSharedWishlist(@PathVariable String token, Model model) {
        Wishlist wishlist = wishlistService.getSharedWishlist(token);
        if (wishlist == null) {
            model.addAttribute("notFound", true);
            return "wishlist-shared";
        }

        List<WishlistItem> items = wishlistService.getWishlistItems(wishlist.getId());
        model.addAttribute("notFound", false);
        model.addAttribute("wishlist", wishlist);
        model.addAttribute("items", items);
        return "wishlist-shared";
    }

    private String renderWishlist(Model model, User user, Wishlist selected) {
        if (selected.isDefaultList()) {
            wishlistService.syncFavoritesToDefault(user, selected);
        }

        model.addAttribute("selectedWishlist", selected);
        model.addAttribute("items", wishlistService.getWishlistItems(selected.getId()));
        model.addAttribute("sharePath",
                selected.getShareToken() == null ? null : "/wishlists/shared/" + selected.getShareToken());
        return "wishlist";
    }

    private User getUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            String username = userDetails.getUsername();
            Optional<User> userOpt = userRepository.findByUsername(username);
            return userOpt.orElse(null);
        }
        return null;
    }
}
