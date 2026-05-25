package com.omar.ecommerce.dtos;

import com.omar.ecommerce.entities.WishlistVisibility;
import lombok.Data;

import java.util.List;

@Data
public class WishlistResponse {
    private Long id;
    private String name;
    private WishlistVisibility visibility;
    private String shareToken;
    private boolean defaultList;
    private Long userId;
    private String username;
    private List<WishlistItemResponse> items;
}
